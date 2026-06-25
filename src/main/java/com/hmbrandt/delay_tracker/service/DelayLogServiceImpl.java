package com.hmbrandt.delay_tracker.service;

import com.hmbrandt.delay_tracker.dto.*;
import com.hmbrandt.delay_tracker.entity.*;
import com.hmbrandt.delay_tracker.repository.DelayLogRepository;
import com.hmbrandt.delay_tracker.repository.OptionItemsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DelayLogServiceImpl implements DelayLogService {

    private final DelayLogRepository delayLogRepository;
    private final OptionItemsRepository optionRepository;

    @Override
    @Transactional
    public DelayLogResponseDto save(DelayLogRequestDto delayDto){
        String currentUser = getUser();

        DelayLog newDelayLog = new DelayLog();
        newDelayLog.setJobId(delayDto.jobId());
        newDelayLog.setEmployeeId(delayDto.employeeId());
        newDelayLog.setDelayDate(delayDto.delayDate());
        newDelayLog.setLocation(delayDto.location());
        newDelayLog.setDelayDescription(delayDto.delayDescription());
        newDelayLog.setImpactEquipment(delayDto.impactEquipment());
        newDelayLog.setSummary(delayDto.summary());
        newDelayLog.setResolution(delayDto.resolution());
        newDelayLog.setWorkers(delayDto.workers());
        newDelayLog.setCost(delayDto.cost());
        newDelayLog.setDelayStatus(delayDto.delayStatus());
        newDelayLog.setCreatedBy(currentUser);
        newDelayLog.setUpdatedBy(currentUser);

        if (delayDto.times() != null) {
            delayDto.times().stream()
                    .map(timeDto -> {
                        DelayTime time = new DelayTime();
                        time.setLogDate(timeDto.logDate());
                        time.setStartTime(timeDto.startTime());
                        time.setEndTime(timeDto.endTime());
                        time.setCreatedBy(currentUser);
                        return time;
                    })
                    .forEach(newDelayLog::addTime); // Uso del helper
        }

        if (delayDto.options() != null) {
            delayDto.options().stream()
                    .map(optionDto -> {
                        DelayOption option = new DelayOption();
                        option.setOptionItemId(optionDto.optionItemId());
                        option.setOther(optionDto.other());
                        option.setCreatedBy(currentUser);
                        return option;
                    })
                    .forEach(newDelayLog::addOption); // Uso del helper
        }

//        if (delayDto.signatures() != null) {
//            delayDto.signatures().stream()
//                    .map(sigDto -> {
//                        DelaySignature signature = new DelaySignature();
//                        signature.setSignatureRole(sigDto.signatureRole());
//                        signature.setCompany(sigDto.company());
//                        signature.setFilePath(sigDto.filePath());
//                        signature.setCreatedBy(currentUser);
//                        return signature;
//                    })
//                    .forEach(newDelayLog::addSignature); // Uso del helper
//        }

        if (delayDto.signatures() != null) {
            List<DelaySignature> signatures = delayDto.signatures().stream().map(sigDto -> {
                DelaySignature signature = new DelaySignature();
                signature.setCompany(sigDto.company());
                signature.setSignatureRole(sigDto.signatureRole());

                // 1. Tomamos el Base64 de 'signatureData', lo guardamos en el disco del VPS
                // y obtenemos la ruta final del archivo físico.
                String storedPath = saveSignatureToDisk(sigDto.signatureData());

                // 2. Guardamos la ruta física en la base de datos
                signature.setFilePath(storedPath);

                signature.setDelayLog(newDelayLog);
                signature.setCreatedBy(currentUser);
                return signature;
            }).toList();

            newDelayLog.setSignatures(signatures);
        }

        DelayLog savedDelayLog = delayLogRepository.save(newDelayLog);

        return mapDelayToDto(savedDelayLog);

    }

    @Override
    @Transactional
    public DelayLogResponseDto update(Long id, DelayLogRequestDto delayDto){
        String currentUser = getUser();

        DelayLog delayLog = delayLogRepository.findById(delayDto.id())
                .orElseThrow(() -> new EntityNotFoundException("Delay order not found with id: "+id));

        delayLog.setJobId(delayDto.jobId());
        delayLog.setEmployeeId(delayDto.employeeId());
        delayLog.setDelayDate(delayDto.delayDate());
        delayLog.setLocation(delayDto.location());
        delayLog.setDelayDescription(delayDto.delayDescription());
        delayLog.setImpactEquipment(delayDto.impactEquipment());
        delayLog.setSummary(delayDto.summary());
        delayLog.setResolution(delayDto.resolution());
        delayLog.setWorkers(delayDto.workers());
        delayLog.setCost(delayDto.cost());
        delayLog.setDelayStatus(delayDto.delayStatus());
        delayLog.setUpdatedBy(currentUser);
        delayLog.setUpdatedAt(LocalDateTime.now());

        if(delayDto.times() != null){
            // Mapeamos los IDs de los tiempos que vienen desde el frontend
            Set<Long> incomingTimeIds = delayDto.times().stream()
                    .map(DelayTimeResponseDto::id)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // PASO A: Identificar y remover los que ya no vienen (Soft Delete automático vía @SQLDelete)
            List<DelayTime> timesToRemove = delayLog.getTimes().stream()
                    .filter(existingTime -> !incomingTimeIds.contains(existingTime.getId()))
                    .toList();
            timesToRemove.forEach(delayLog::removeTime);

            // PASO B: Procesar los elementos del DTO
            delayDto.times().forEach(timeDto -> {
                if (timeDto.id() != null) {
                    // Buscamos si el registro existente cambió en algo
                    delayLog.getTimes().stream()
                            .filter(t -> t.getId().equals(timeDto.id()))
                            .findFirst()
                            .ifPresent(existingTime -> {
                                // Si cambió alguna propiedad, aplicamos Soft Delete al viejo y creamos uno nuevo
                                if (!Objects.equals(existingTime.getStartTime(), timeDto.startTime()) ||
                                        !Objects.equals(existingTime.getEndTime(), timeDto.endTime()) ||
                                        !Objects.equals(existingTime.getLogDate(), timeDto.logDate())) {

                                    // 1. Eliminar el viejo (Esto pondrá su deleted_at = NOW())
                                    delayLog.removeTime(existingTime);

                                    // 2. Crear el nuevo con los datos actualizados
                                    DelayTime newTime = new DelayTime();
                                    newTime.setLogDate(timeDto.logDate());
                                    newTime.setStartTime(timeDto.startTime());
                                    newTime.setEndTime(timeDto.endTime());
                                    newTime.setCreatedBy(currentUser);
                                    delayLog.addTime(newTime);
                                }
                            });
                } else {
                    // Es un registro completamente nuevo desde el formulario
                    DelayTime newTime = new DelayTime();
                    newTime.setLogDate(timeDto.logDate());
                    newTime.setStartTime(timeDto.startTime());
                    newTime.setEndTime(timeDto.endTime());
                    newTime.setCreatedBy(currentUser);
                    delayLog.addTime(newTime);
                }
            });
        }

        // 4. Sincronizar Colección: Opciones (DelayOptions)
        if (delayDto.options() != null) {
            Set<Long> incomingOptionIds = delayDto.options().stream()
                    .map(DelayOptionResponseDto::id)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            List<DelayOption> optionsToRemove = delayLog.getOptions().stream()
                    .filter(existingOpt -> !incomingOptionIds.contains(existingOpt.getId()))
                    .toList();
            optionsToRemove.forEach(delayLog::removeOption);

            delayDto.options().forEach(optionDto -> {
                if (optionDto.id() != null) {
                    delayLog.getOptions().stream()
                            .filter(o -> o.getId().equals(optionDto.id()))
                            .findFirst()
                            .ifPresent(existingOpt -> {
                                if (!existingOpt.getOptionItemId().equals(optionDto.optionItemId()) ||
                                        !Objects.equals(existingOpt.getOther(), optionDto.other())) {

                                    delayLog.removeOption(existingOpt);

                                    DelayOption newOption = new DelayOption();
                                    newOption.setOptionItemId(optionDto.optionItemId());
                                    newOption.setOther(optionDto.other());
                                    newOption.setCreatedBy(currentUser);
                                    delayLog.addOption(newOption);
                                }
                            });
                } else {
                    DelayOption newOption = new DelayOption();
                    newOption.setOptionItemId(optionDto.optionItemId());
                    newOption.setOther(optionDto.other());
                    newOption.setCreatedBy(currentUser);
                    delayLog.addOption(newOption);
                }
            });
        }

        // 5. Sincronizar Colección: Firmas (DelaySignatures)
//        if (delayDto.signatures() != null) {
//            Set<Long> incomingSigIds = delayDto.signatures().stream()
//                    .map(DelaySignatureResponseDto::id)
//                    .filter(Objects::nonNull)
//                    .collect(Collectors.toSet());
//
//            List<DelaySignature> sigsToRemove = delayLog.getSignatures().stream()
//                    .filter(existingSig -> !incomingSigIds.contains(existingSig.getId()))
//                    .toList();
//            sigsToRemove.forEach(delayLog::removeSignature);
//
//            delayDto.signatures().forEach(sigDto -> {
//                if (sigDto.id() != null) {
//                    delayLog.getSignatures().stream()
//                            .filter(s -> s.getId().equals(sigDto.id()))
//                            .findFirst()
//                            .ifPresent(existingSig -> {
//                                if (!existingSig.getSignatureRole().equals(sigDto.signatureRole()) ||
//                                        !existingSig.getCompany().equals(sigDto.company()) ||
//                                        !existingSig.getFilePath().equals(sigDto.filePath())) {
//
//                                    delayLog.removeSignature(existingSig);
//
//                                    DelaySignature newSig = new DelaySignature();
//                                    newSig.setSignatureRole(sigDto.signatureRole());
//                                    newSig.setCompany(sigDto.company());
//                                    newSig.setFilePath(sigDto.filePath());
//                                    newSig.setCreatedBy(currentUser);
//                                    delayLog.addSignature(newSig);
//                                }
//                            });
//                } else {
//                    DelaySignature newSig = new DelaySignature();
//                    newSig.setSignatureRole(sigDto.signatureRole());
//                    newSig.setCompany(sigDto.company());
//                    newSig.setFilePath(sigDto.filePath());
//                    newSig.setCreatedBy(currentUser);
//                    delayLog.addSignature(newSig);
//                }
//            });
//        }

        if (delayDto.signatures() != null) {
            // 1. Identificar y borrar del disco físico las firmas que van a desaparecer
            if (delayLog.getSignatures() != null) {
                Set<String> incomingUrls = delayDto.signatures().stream()
                        .map(sigDto -> sigDto.signatureData()) // Puede ser URL o Base64
                        .collect(Collectors.toSet());

                delayLog.getSignatures().forEach(existingSig -> {
                    // Si la URL actual en la BD no está en lo que viene del frontend, se borra el archivo
                    if (!incomingUrls.contains(existingSig.getFilePath())) {
                        deleteSignatureFromDisk(existingSig.getFilePath());
                    }
                });

                // Ahora sí limpiamos la colección en JPA
                delayLog.getSignatures().clear();
            } else {
                delayLog.setSignatures(new ArrayList<>());
            }

            // 2. Mapear y procesar las firmas entrantes
            List<DelaySignature> newSignatures = delayDto.signatures().stream().map(sigDto -> {
                DelaySignature signature = new DelaySignature();
                signature.setSignatureRole(sigDto.signatureRole());
                signature.setCompany(sigDto.company());

                // Si es Base64 crea archivo nuevo. Si es URL, mantiene la existente.
                String storedPath = saveSignatureToDisk(sigDto.signatureData());

                signature.setFilePath(storedPath);
                signature.setDelayLog(delayLog);
                signature.setCreatedBy(currentUser);
                return signature;
            }).toList();

            // 3. Asignamos las firmas actualizadas
            delayLog.getSignatures().addAll(newSignatures);
        }

        DelayLog updatedDelayLog = delayLogRepository.save(delayLog);
        return mapDelayToDto(updatedDelayLog);
    }

    @Override
    @Transactional(readOnly = true)
    public DelayLogResponseDto findById(Long id){
        return delayLogRepository.findById(id)
                .map(this::mapDelayToDto)
                .orElseThrow(() -> new EntityNotFoundException("Delay log not fount with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DelayLogResponseDto> findByJobId(Long jobId){
        return delayLogRepository.findByJobId(jobId)
                .stream()
                .map(this::mapDelayToDto)
                .toList();
    }

    public void delete(Long id){
        if(!delayLogRepository.existsById(id)){
            throw new EntityNotFoundException("Id not found");
        }

        delayLogRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OptionsItemRequestDto> findOptions(){
        List<OptionItem> options = optionRepository.findAll();

        return options.stream()
                .map(this::mapOptionDto)
                .toList();
    }

    @Override
    @Transactional
    public DelayLogResponseDto finalizeOrder(Long delayId){
        String currentUser = "SYSTEM_FALLBACK"; // Valor por defecto por si falla

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            currentUser = authentication.getName();
        } else {
            currentUser = "SYSTEM_FALLBACK";
            System.out.println(">>> ALERTA: La petición llegó SIN autenticación o el contexto es NULL");
        }

        DelayLog delay = delayLogRepository.findById(delayId)
                .orElseThrow(() -> new  EntityNotFoundException("order not found with id: " + delayId));

        delay.setDelayStatus("FINALIZED");
        delay.setUpdatedBy(currentUser);

        delayLogRepository.save(delay);
        return mapDelayToDto(delay);
    }

    @Value("${application.upload.dir}")
    private String uploadDir;

    private String saveSignatureToDisk(String base64Image) {
        if (base64Image == null || base64Image.isEmpty()) {
            return null;
        }

        if (!base64Image.contains("data:image") && !base64Image.contains(";base64,")) {
            return base64Image;
        }

        try {
            // 1. Limpiar el prefijo de Base64 si React lo envía completo (data:image/png;base64,...)
            String cleanBase64 = base64Image.split(",")[1];

            // 2. Decodificar los bytes de la imagen
            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

            // 3. Definir la ruta del VPS (¡Usa variables de configuración en tu application.yml!)
            // Ejemplo local windows/mac o absoluto en linux del VPS: "/var/www/uploads/signatures/"
            Path directoryPath = Paths.get(uploadDir);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath); // Crea las carpetas necesarias en C:/ o /var/
            }

            // 4. Crear un nombre de archivo único para evitar duplicados
            String fileName = UUID.randomUUID().toString() + ".png";
            Path targetFilePath = directoryPath.resolve(fileName);

            // 5. Escribir los bytes en el archivo físico
            Files.write(targetFilePath, imageBytes);

            // 6. Retornar el path relativo o el nombre que guardarás en la BD
            return "/uploads/delay-log/signatures/" + fileName;

        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException("Issue savin signature in file system: " + e.getMessage());
        }
    }

    private void deleteSignatureFromDisk(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains("/")) return;

        try {
            // Extrae el nombre del archivo (ej: nombre-uuid.png) de la URL guardada en la BD
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            Path targetFilePath = Paths.get(uploadDir).resolve(fileName);

            // Lo elimina físicamente si existe
            Files.deleteIfExists(targetFilePath);
        } catch (IOException e) {
            // Loggeamos el error pero no bloqueamos la app por un fallo de limpieza
            System.err.println("No se pudo eliminar el archivo físico: " + e.getMessage());
        }
    }

    private OptionsItemRequestDto mapOptionDto(OptionItem option){
        return new OptionsItemRequestDto(
                option.getId(),
                option.getOptionType(),
                option.getOptionName()
        );
    }

    private String getUser(){
        String currentUser = "SYSTEM_FALLBACK";

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            currentUser = authentication.getName();
        } else {
            System.out.println(">>> ALERT: no authenticated or context NULL");
        }

        return currentUser;
    }

    private DelayLogResponseDto mapDelayToDto(DelayLog entity){
        return new DelayLogResponseDto(
                entity.getId(),
                entity.getJobId(),
                entity.getEmployeeId(),
                entity.getDelayDate(),
                entity.getLocation(),
                entity.getDelayDescription(),
                entity.getImpactEquipment(),
                entity.getSummary(),
                entity.getResolution(),
                entity.getWorkers(),
                entity.getCost(),
                entity.getDelayStatus(),
                entity.getTimes()
                        .stream()
                        .map(this::mapTimesToDto)
                        .collect(Collectors.toList()),
                entity.getOptions()
                        .stream()
                        .map(this::mapOptionsToDto)
                        .collect(Collectors.toList()),
                entity.getSignatures()
                        .stream()
                        .map(this::mapSignatureToDto)
                        .collect(Collectors.toList())
        );
    }

    private DelayTimeResponseDto mapTimesToDto(DelayTime entity){
        return new DelayTimeResponseDto(
                entity.getId(),
                entity.getLogDate(),
                entity.getStartTime(),
                entity.getEndTime()
        );
    }

    private DelayOptionResponseDto mapOptionsToDto(DelayOption entity){
        return new DelayOptionResponseDto(
                entity.getId(),
                entity.getOptionItemId(),
                entity.getOther()
        );
    }

    private DelaySignatureResponseDto mapSignatureToDto(DelaySignature entity){
        return new DelaySignatureResponseDto(
                entity.getId(),
                entity.getSignatureRole(),
                entity.getCompany(),
                entity.getFilePath()
        );
    }



}
