package com.bishe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.common.Result;
import com.bishe.entity.*;
import com.bishe.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 溯源与生命周期统一控制器
 * 包含: 流转记录, 上链交易, 回收评估, 维修记录, 车辆信息
 */
@RestController
@RequestMapping("/trace")
public class TraceabilityController {

    @Autowired
    private IBatteryTransferRecordService transferService;
    @Autowired
    private IChainTransactionService chainService;
    @Autowired
    private IRecyclingAppraisalService recyclingService;
    @Autowired
    private IMaintenanceRecordService maintenanceService;
    @Autowired
    private IVehicleInfoService vehicleService;
    @Autowired
    private ISalesRecordService salesService;
    @Autowired
    private ISysAuditService sysAuditService;
    @Autowired
    private ISysUserService sysUserService;
    @Autowired
    private ISysRoleService sysRoleService;
    @Autowired
    private IBatteryInfoService batteryInfoService;

    private Long getCurrentUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj instanceof Long userId) {
            return userId;
        }
        if (userIdObj instanceof Number num) {
            return num.longValue();
        }
        return null;
    }

    private boolean hasAnyRole(Long userId, String... roleKeys) {
        if (userId == null || roleKeys == null || roleKeys.length == 0) {
            return false;
        }
        java.util.Set<String> roles = sysRoleService.getRoleKeysByUserId(userId);
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        for (String roleKey : roleKeys) {
            if (StringUtils.hasText(roleKey) && roles.contains(roleKey)) {
                return true;
            }
        }
        return false;
    }

    private String getCurrentUserDisplayName(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return null;
        }
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    private <T> Result<T> forbidden() {
        return Result.error(403, "无权限访问");
    }

    // ==================== 流转记录 (BatteryTransferRecord) ====================

    @GetMapping("/transfer/list")
    public Result<Page<BatteryTransferRecord>> listTransfer(@RequestParam(defaultValue = "1") Integer pageNum,
                                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                                            @RequestParam(required = false) String batteryId) {
        Page<BatteryTransferRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BatteryTransferRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(batteryId), BatteryTransferRecord::getBatteryId, batteryId);
        wrapper.orderByDesc(BatteryTransferRecord::getCreateTime);
        return Result.success(transferService.page(page, wrapper));
    }

    @PostMapping("/transfer")
    public Result<Boolean> saveTransfer(@RequestBody BatteryTransferRecord record) {
        if (record == null) {
            return Result.error(400, "请求数据不能为空");
        }
        if (!org.springframework.util.StringUtils.hasText(record.getBatteryId())) {
            return Result.error(400, "电池ID不能为空");
        }
        if (record.getFromOwner() == null) {
            return Result.error(400, "原拥有者ID不能为空");
        }
        if (record.getToOwner() == null) {
            return Result.error(400, "新拥有者ID不能为空");
        }

        boolean saved = transferService.save(record);
        if (!saved) {
            return Result.success(false);
        }

        String batteryId = record.getBatteryId();
        Long toOwner = record.getToOwner();
        if (org.springframework.util.StringUtils.hasText(batteryId) && toOwner != null) {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VehicleInfo> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.eq(VehicleInfo::getBatteryId, batteryId.trim())
                    .orderByDesc(VehicleInfo::getBindTime)
                    .last("limit 1");
            VehicleInfo vehicle = vehicleService.getOne(wrapper);
            if (vehicle != null) {
                VehicleInfo update = new VehicleInfo();
                update.setVehicleId(vehicle.getVehicleId());
                update.setOwnerId(toOwner);
                vehicleService.updateById(update);
            }
        }

        return Result.success(true);
    }

    // ==================== 上链交易 (ChainTransaction) ====================

    @GetMapping("/chain/list")
    public Result<Page<ChainTransaction>> listChain(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    @RequestParam(required = false) String txHash,
                                                    @RequestParam(required = false) String methodName,
                                                    @RequestParam(required = false) String batteryId) {
        Page<ChainTransaction> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ChainTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(txHash), ChainTransaction::getTxHash, txHash);
        wrapper.eq(StringUtils.hasText(methodName), ChainTransaction::getMethodName, methodName);
        if (StringUtils.hasText(batteryId)) {
            String id = batteryId.trim();
            wrapper.and(w -> w.like(ChainTransaction::getParams, "\"batteryId\":\"" + id + "\"")
                    .or()
                    .like(ChainTransaction::getParams, "\"battery_id\":\"" + id + "\""));
        }
        wrapper.orderByDesc(ChainTransaction::getCreateTime);
        return Result.success(chainService.page(page, wrapper));
    }

    // ==================== 回收评估 (RecyclingAppraisal) ====================

    @GetMapping("/recycling/list")
    public Result<Page<RecyclingAppraisal>> listRecycling(@RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                                          @RequestParam(required = false) String batteryId,
                                                          @RequestParam(required = false) Integer status,
                                                          @RequestParam(required = false) String recycleNo,
                                                          HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "recycler", "maintainer", "maintenance", "manufacturer", "dealer", "owner")) {
            return forbidden();
        }
        Page<RecyclingAppraisal> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RecyclingAppraisal> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(batteryId), RecyclingAppraisal::getBatteryId, batteryId);
        wrapper.eq(status != null, RecyclingAppraisal::getStatus, status);
        wrapper.like(StringUtils.hasText(recycleNo), RecyclingAppraisal::getRecycleNo, recycleNo);
        wrapper.orderByDesc(RecyclingAppraisal::getApplyTime);
        return Result.success(recyclingService.page(page, wrapper));
    }

    @PostMapping("/recycling")
    public Result<Boolean> saveRecycling(@RequestBody RecyclingAppraisal appraisal, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin")) {
            return forbidden();
        }
        return Result.success(recyclingService.save(appraisal));
    }

    @PutMapping("/recycling")
    public Result<Boolean> updateRecycling(@RequestBody RecyclingAppraisal appraisal, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin")) {
            return forbidden();
        }
        return Result.success(recyclingService.updateById(appraisal));
    }

    @PostMapping("/recycling/apply")
    @Transactional(rollbackFor = Exception.class)
    public Result<RecyclingAppraisal> applyRecycling(@RequestBody RecyclingApplyDto dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "maintainer", "maintenance")) {
            return forbidden();
        }
        if (dto == null || !StringUtils.hasText(dto.getBatteryId())) {
            return Result.error(400, "电池ID不能为空");
        }
        if (!StringUtils.hasText(dto.getAppearance())) {
            return Result.error(400, "外观描述不能为空");
        }
        if (!StringUtils.hasText(dto.getSuggestion())) {
            return Result.error(400, "处理建议不能为空");
        }
        String reason = dto.getReason();
        if (!StringUtils.hasText(reason)) {
            return Result.error(400, "回收原因不能为空");
        }
        BatteryInfo battery = batteryInfoService.getById(dto.getBatteryId());
        if (battery == null) {
            return Result.error(400, "电池不存在");
        }
        Integer s = battery.getStatus();
        if (s != null && (s == 0 || s == 3 || s == 6)) {
            return Result.error(400, "当前电池状态不允许申请回收");
        }

        LambdaQueryWrapper<RecyclingAppraisal> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(RecyclingAppraisal::getBatteryId, dto.getBatteryId());
        existingWrapper.in(RecyclingAppraisal::getStatus, 0, 1, 3);
        if (recyclingService.count(existingWrapper) > 0) {
            return Result.error(400, "该电池已存在进行中的回收流程");
        }

        SysUser user = sysUserService.getById(userId);
        Long recyclerId = user != null ? user.getDeptId() : null;
        String applyUser = getCurrentUserDisplayName(request);
        if (!StringUtils.hasText(applyUser)) {
            applyUser = "system";
        }

        RecyclingAppraisal appraisal = new RecyclingAppraisal();
        appraisal.setBatteryId(dto.getBatteryId());
        appraisal.setRecyclerId(recyclerId);
        appraisal.setAppearance(dto.getAppearance());
        appraisal.setSuggestion(dto.getSuggestion());
        appraisal.setAppraiser(applyUser);
        appraisal.setCreateTime(LocalDateTime.now());
        appraisal.setStatus(0);
        appraisal.setApplyReason(reason.trim());
        appraisal.setApplyUser(applyUser);
        appraisal.setApplyTime(LocalDateTime.now());
        appraisal.setSnapshotVoltage(battery.getVoltage());
        appraisal.setSnapshotCapacity(battery.getCapacity());
        appraisal.setSnapshotBatteryStatus(battery.getStatus());
        appraisal.setPhotoUrls("[]");
        appraisal.setUpdateTime(LocalDateTime.now());

        boolean saved = recyclingService.save(appraisal);
        if (!saved || appraisal.getAppraisalId() == null) {
            return Result.error(500, "回收申请创建失败");
        }
        sysAuditService.submitAudit("RECYCLING", appraisal.getAppraisalId().toString(), applyUser);

        java.util.Map<String, Object> applyPayload = new java.util.HashMap<>();
        applyPayload.put("appraisalId", appraisal.getAppraisalId());
        applyPayload.put("batteryId", appraisal.getBatteryId());
        applyPayload.put("applyUser", applyUser);
        applyPayload.put("applyTime", appraisal.getApplyTime());
        applyPayload.put("applyReason", appraisal.getApplyReason());
        applyPayload.put("snapshotVoltage", appraisal.getSnapshotVoltage());
        applyPayload.put("snapshotCapacity", appraisal.getSnapshotCapacity());
        applyPayload.put("snapshotBatteryStatus", appraisal.getSnapshotBatteryStatus());
        String applyTxHash = chainService.submitTransaction(
                "submitRecyclingApply",
                cn.hutool.json.JSONUtil.toJsonStr(applyPayload)
        );

        if (StringUtils.hasText(applyTxHash)) {
            RecyclingAppraisal update = new RecyclingAppraisal();
            update.setAppraisalId(appraisal.getAppraisalId());
            update.setDataHash(applyTxHash);
            update.setUpdateTime(LocalDateTime.now());
            recyclingService.updateById(update);
            appraisal.setDataHash(applyTxHash);
        }
        return Result.success(appraisal);
    }

    @PostMapping("/recycling/audit")
    public Result<Boolean> auditRecycling(@RequestBody RecyclingAuditDto dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "recycler")) {
            return forbidden();
        }
        if (dto == null || dto.getAppraisalId() == null) {
            return Result.error(400, "评估ID不能为空");
        }
        if (dto.getStatus() == null || (dto.getStatus() != 1 && dto.getStatus() != 2)) {
            return Result.error(400, "审核状态不合法");
        }
        String auditor = getCurrentUserDisplayName(request);
        if (!StringUtils.hasText(auditor)) {
            auditor = "system";
        }

        LambdaQueryWrapper<SysAudit> query = new LambdaQueryWrapper<>();
        query.eq(SysAudit::getBusinessType, "RECYCLING");
        query.eq(SysAudit::getBusinessId, dto.getAppraisalId().toString());
        query.eq(SysAudit::getStatus, 0);
        query.orderByDesc(SysAudit::getApplyTime);
        query.last("LIMIT 1");
        SysAudit audit = sysAuditService.getOne(query);
        if (audit == null) {
            RecyclingAppraisal appraisal = recyclingService.getById(dto.getAppraisalId());
            if (appraisal == null) {
                return Result.error(400, "回收申请不存在");
            }
            if (appraisal.getStatus() == null || appraisal.getStatus() != 0) {
                return Result.error(400, "回收申请不处于待审核状态");
            }
            String applyUser = appraisal.getApplyUser();
            if (!StringUtils.hasText(applyUser)) {
                applyUser = "system";
            }
            sysAuditService.submitAudit("RECYCLING", dto.getAppraisalId().toString(), applyUser);

            SysAudit created = sysAuditService.getOne(query);
            if (created == null) {
                return Result.error(400, "未找到待审核的回收申请");
            }
            audit = created;
        }
        sysAuditService.doAudit(audit.getAuditId(), dto.getStatus(), dto.getAuditOpinion(), auditor);
        return Result.success(true);
    }

    @PostMapping("/recycling/photo/upload")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> uploadRecyclingPhoto(@RequestParam("appraisalId") Long appraisalId,
                                               @RequestParam("file") MultipartFile file,
                                               HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "recycler", "maintainer", "maintenance")) {
            return forbidden();
        }
        if (appraisalId == null) {
            return Result.error(400, "评估ID不能为空");
        }
        if (file == null || file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            return Result.error(400, "文件大小不能超过10MB");
        }
        RecyclingAppraisal appraisal = recyclingService.getById(appraisalId);
        if (appraisal == null) {
            return Result.error(400, "回收申请不存在");
        }
        if (appraisal.getStatus() == null || appraisal.getStatus() != 1) {
            return Result.error(400, "仅允许对已通过审核的回收申请上传照片");
        }

        String originalFilename = file.getOriginalFilename();
        String safeName = (originalFilename == null ? "file" : originalFilename)
                .replace("\\", "_")
                .replace("/", "_")
                .replace("..", "_");
        String savedName = UUID.randomUUID().toString().replace("-", "") + "_" + safeName;
        Path targetDir = Paths.get(System.getProperty("user.dir"), "uploads", "recycling", "photos", String.valueOf(appraisalId));
        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(savedName);
            file.transferTo(targetFile.toFile());
            String url = "/files/recycling/photos/" + appraisalId + "/" + savedName;

            List<String> urls = parseJsonStringList(appraisal.getPhotoUrls());
            urls.add(url);

            RecyclingAppraisal update = new RecyclingAppraisal();
            update.setAppraisalId(appraisalId);
            update.setPhotoUrls(cn.hutool.json.JSONUtil.toJsonStr(urls));
            update.setUpdateTime(LocalDateTime.now());
            recyclingService.updateById(update);

            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("appraisalId", appraisalId);
            payload.put("batteryId", appraisal.getBatteryId());
            payload.put("operator", getCurrentUserDisplayName(request));
            payload.put("url", url);
            payload.put("time", LocalDateTime.now());
            chainService.submitTransaction("uploadRecyclingPhoto", cn.hutool.json.JSONUtil.toJsonStr(payload));
            return Result.success(url);
        } catch (Exception e) {
            return Result.error(500, "上传失败");
        }
    }

    @PostMapping("/recycling/report/upload")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> uploadRecyclingReport(@RequestParam("appraisalId") Long appraisalId,
                                                @RequestParam("file") MultipartFile file,
                                                HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "recycler", "maintainer", "maintenance")) {
            return forbidden();
        }
        if (appraisalId == null) {
            return Result.error(400, "评估ID不能为空");
        }
        if (file == null || file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            return Result.error(400, "文件大小不能超过10MB");
        }
        RecyclingAppraisal appraisal = recyclingService.getById(appraisalId);
        if (appraisal == null) {
            return Result.error(400, "回收申请不存在");
        }
        if (appraisal.getStatus() == null || appraisal.getStatus() != 1) {
            return Result.error(400, "仅允许对已通过审核的回收申请上传检测报告");
        }

        String originalFilename = file.getOriginalFilename();
        String safeName = (originalFilename == null ? "file" : originalFilename)
                .replace("\\", "_")
                .replace("/", "_")
                .replace("..", "_");
        String savedName = UUID.randomUUID().toString().replace("-", "") + "_" + safeName;
        Path targetDir = Paths.get(System.getProperty("user.dir"), "uploads", "recycling", "reports", String.valueOf(appraisalId));
        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(savedName);
            file.transferTo(targetFile.toFile());
            String url = "/files/recycling/reports/" + appraisalId + "/" + savedName;

            RecyclingAppraisal update = new RecyclingAppraisal();
            update.setAppraisalId(appraisalId);
            update.setPerformanceReportUrl(url);
            update.setUpdateTime(LocalDateTime.now());
            recyclingService.updateById(update);

            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("appraisalId", appraisalId);
            payload.put("batteryId", appraisal.getBatteryId());
            payload.put("operator", getCurrentUserDisplayName(request));
            payload.put("url", url);
            payload.put("time", LocalDateTime.now());
            chainService.submitTransaction("uploadRecyclingReport", cn.hutool.json.JSONUtil.toJsonStr(payload));
            return Result.success(url);
        } catch (Exception e) {
            return Result.error(500, "上传失败");
        }
    }

    @PostMapping("/recycling/valuation/calc")
    @Transactional(rollbackFor = Exception.class)
    public Result<RecyclingAppraisal> calcRecyclingValuation(@RequestBody RecyclingValuationCalcDto dto,
                                                             HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "recycler")) {
            return forbidden();
        }
        if (dto == null || dto.getAppraisalId() == null) {
            return Result.error(400, "评估ID不能为空");
        }
        RecyclingAppraisal appraisal = recyclingService.getById(dto.getAppraisalId());
        if (appraisal == null) {
            return Result.error(400, "回收申请不存在");
        }
        if (appraisal.getStatus() == null || appraisal.getStatus() != 1) {
            return Result.error(400, "回收申请未通过审核");
        }
        List<String> photos = parseJsonStringList(appraisal.getPhotoUrls());
        if (photos.size() < 3) {
            return Result.error(400, "外观拍照需至少3张不同角度");
        }
        if (!StringUtils.hasText(appraisal.getPerformanceReportUrl())) {
            return Result.error(400, "请先上传完整性能检测报告");
        }

        String batteryId = appraisal.getBatteryId();
        BatteryInfo battery = StringUtils.hasText(batteryId) ? batteryInfoService.getById(batteryId) : null;
        BigDecimal capacity = battery != null ? battery.getCapacity() : null;
        if (capacity == null) {
            capacity = appraisal.getSnapshotCapacity();
        }
        if (capacity == null || capacity.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error(400, "缺少容量数据，无法计算预估价");
        }

        Integer batteryStatus = battery != null ? battery.getStatus() : null;
        if (batteryStatus == null) {
            batteryStatus = appraisal.getSnapshotBatteryStatus();
        }

        long maintenanceCount = 0;
        long transferCount = 0;
        if (StringUtils.hasText(batteryId)) {
            maintenanceCount = maintenanceService.count(new LambdaQueryWrapper<MaintenanceRecord>()
                    .eq(MaintenanceRecord::getBatteryId, batteryId));
            transferCount = transferService.count(new LambdaQueryWrapper<BatteryTransferRecord>()
                    .eq(BatteryTransferRecord::getBatteryId, batteryId));
        }

        double statusFactor;
        if (batteryStatus == null) {
            statusFactor = 0.8;
        } else {
            statusFactor = switch (batteryStatus) {
                case 1 -> 0.95;
                case 2 -> 0.90;
                case 3 -> 0.80;
                case 4 -> 0.75;
                case 5, 6 -> 0.70;
                case 7 -> 0.65;
                default -> 0.80;
            };
        }
        double maintenanceFactor = Math.max(0.5, 1 - Math.min(maintenanceCount * 0.06, 0.36));
        double transferFactor = Math.max(0.6, 1 - Math.min(transferCount * 0.03, 0.21));
        BigDecimal preliminary = capacity
                .multiply(BigDecimal.valueOf(80))
                .multiply(BigDecimal.valueOf(statusFactor))
                .multiply(BigDecimal.valueOf(maintenanceFactor))
                .multiply(BigDecimal.valueOf(transferFactor))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        if (preliminary.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error(400, "预估价计算失败");
        }

        LambdaQueryWrapper<MaintenanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(batteryId), MaintenanceRecord::getBatteryId, batteryId);
        wrapper.orderByDesc(MaintenanceRecord::getCreateTime);
        List<MaintenanceRecord> history = maintenanceService.list(wrapper);
        String historyJson = cn.hutool.json.JSONUtil.toJsonStr(history == null ? java.util.Collections.emptyList() : history);

        String operator = getCurrentUserDisplayName(request);
        if (!StringUtils.hasText(operator)) {
            operator = "system";
        }
        String basis = ("auto; operator=" + operator + "; time=" + LocalDateTime.now() + "; batteryId=" + (batteryId == null ? "" : batteryId)
                + "; maintenanceCount=" + maintenanceCount + "; transferCount=" + transferCount
                + "; batteryStatus=" + (batteryStatus == null ? "" : batteryStatus)).trim();

        RecyclingAppraisal update = new RecyclingAppraisal();
        update.setAppraisalId(appraisal.getAppraisalId());
        update.setPreliminaryValue(preliminary);
        update.setResidualValue(preliminary);
        update.setMaintenanceHistoryJson(historyJson);
        update.setValuationBasis(basis);
        update.setUpdateTime(LocalDateTime.now());
        recyclingService.updateById(update);

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("appraisalId", appraisal.getAppraisalId());
        payload.put("batteryId", appraisal.getBatteryId());
        payload.put("preliminaryValue", preliminary);
        payload.put("valuationBasis", basis);
        payload.put("operator", getCurrentUserDisplayName(request));
        payload.put("time", LocalDateTime.now());
        chainService.submitTransaction("saveRecyclingValuation", cn.hutool.json.JSONUtil.toJsonStr(payload));

        RecyclingAppraisal updated = recyclingService.getById(appraisal.getAppraisalId());
        return Result.success(updated);
    }


    @PostMapping("/recycling/valuation/confirm")
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> confirmRecyclingPrice(@RequestBody RecyclingValuationConfirmDto dto,
                                                 HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "recycler")) {
            return forbidden();
        }
        if (dto == null || dto.getAppraisalId() == null) {
            return Result.error(400, "评估ID不能为空");
        }
        if (dto.getFinalValue() == null || dto.getFinalValue().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error(400, "最终回收价格必须大于0");
        }
        RecyclingAppraisal appraisal = recyclingService.getById(dto.getAppraisalId());
        if (appraisal == null) {
            return Result.error(400, "回收申请不存在");
        }
        if (appraisal.getStatus() == null || appraisal.getStatus() != 1) {
            return Result.error(400, "回收申请未通过审核");
        }
        if (appraisal.getPreliminaryValue() == null) {
            return Result.error(400, "请先完成初步估值计算");
        }
        List<String> photos = parseJsonStringList(appraisal.getPhotoUrls());
        if (photos.size() < 3 || !StringUtils.hasText(appraisal.getPerformanceReportUrl())) {
            return Result.error(400, "估值前置步骤未完成");
        }

        String reviewer = getCurrentUserDisplayName(request);
        if (!StringUtils.hasText(reviewer)) {
            reviewer = "system";
        }

        LocalDateTime now = LocalDateTime.now();
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("appraisalId", appraisal.getAppraisalId());
        payload.put("batteryId", appraisal.getBatteryId());
        payload.put("recycleNo", appraisal.getRecycleNo());
        payload.put("preliminaryValue", appraisal.getPreliminaryValue());
        payload.put("finalValue", dto.getFinalValue());
        payload.put("priceReviewer", reviewer);
        payload.put("time", now);
        String receiptHash = chainService.submitTransaction(
                "confirmRecyclingPrice",
                cn.hutool.json.JSONUtil.toJsonStr(payload)
        );

        RecyclingAppraisal update = new RecyclingAppraisal();
        update.setAppraisalId(appraisal.getAppraisalId());
        update.setFinalValue(dto.getFinalValue().setScale(2, java.math.RoundingMode.HALF_UP));
        update.setPriceReviewer(reviewer);
        update.setRecycleTime(now);
        update.setReceiptHash(receiptHash);
        update.setStatus(3);
        update.setUpdateTime(now);
        boolean ok = recyclingService.updateById(update);

        if (ok && StringUtils.hasText(appraisal.getBatteryId())) {
            BatteryInfo batteryInfo = new BatteryInfo();
            batteryInfo.setBatteryId(appraisal.getBatteryId());
            batteryInfo.setStatus(7);
            batteryInfoService.updateById(batteryInfo);
        }
        return Result.success(ok);
    }

    @GetMapping("/recycling/receipt/{appraisalId}")
    @Transactional(rollbackFor = Exception.class)
    public Result<java.util.Map<String, Object>> getRecyclingReceipt(@PathVariable("appraisalId") Long appraisalId,
                                                                     HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "recycler", "maintainer", "maintenance")) {
            return forbidden();
        }
        if (appraisalId == null) {
            return Result.error(400, "评估ID不能为空");
        }
        RecyclingAppraisal appraisal = recyclingService.getById(appraisalId);
        if (appraisal == null) {
            return Result.error(400, "回收申请不存在");
        }

        LambdaQueryWrapper<SysAudit> auditQuery = new LambdaQueryWrapper<>();
        auditQuery.eq(SysAudit::getBusinessType, "RECYCLING");
        auditQuery.eq(SysAudit::getBusinessId, appraisalId.toString());
        auditQuery.orderByDesc(SysAudit::getApplyTime);
        auditQuery.last("LIMIT 1");
        SysAudit audit = sysAuditService.getOne(auditQuery);

        BatteryInfo battery = StringUtils.hasText(appraisal.getBatteryId()) ? batteryInfoService.getById(appraisal.getBatteryId()) : null;

        java.util.Map<String, Object> receipt = new java.util.HashMap<>();
        receipt.put("recycleNo", appraisal.getRecycleNo());
        receipt.put("recycleTime", appraisal.getRecycleTime());
        receipt.put("batteryInfo", battery);
        receipt.put("preliminaryValue", appraisal.getPreliminaryValue());
        receipt.put("finalValue", appraisal.getFinalValue());
        receipt.put("valuationBasis", appraisal.getValuationBasis());
        receipt.put("auditor", audit != null ? audit.getAuditor() : null);
        receipt.put("auditTime", audit != null ? audit.getAuditTime() : null);
        receipt.put("auditOpinion", audit != null ? audit.getAuditOpinion() : null);
        receipt.put("receiptHash", appraisal.getReceiptHash());

        if (!StringUtils.hasText(appraisal.getReceiptHash())) {
            String hash = chainService.submitTransaction("generateRecyclingReceipt", cn.hutool.json.JSONUtil.toJsonStr(receipt));
            RecyclingAppraisal update = new RecyclingAppraisal();
            update.setAppraisalId(appraisalId);
            update.setReceiptHash(hash);
            update.setUpdateTime(LocalDateTime.now());
            recyclingService.updateById(update);
            receipt.put("receiptHash", hash);
        }

        return Result.success(receipt);
    }

    private List<String> parseJsonStringList(String text) {
        if (!StringUtils.hasText(text)) {
            return new java.util.ArrayList<>();
        }
        try {
            cn.hutool.json.JSONArray arr = cn.hutool.json.JSONUtil.parseArray(text);
            return arr.toList(String.class);
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    // ==================== 维修记录 (MaintenanceRecord) ====================

    @GetMapping("/maintenance/list")
    public Result<Page<MaintenanceRecord>> listMaintenance(@RequestParam(defaultValue = "1") Integer pageNum,
                                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                                           @RequestParam(required = false) String batteryId) {
        Page<MaintenanceRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MaintenanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(batteryId), MaintenanceRecord::getBatteryId, batteryId);
        wrapper.orderByDesc(MaintenanceRecord::getCreateTime);
        return Result.success(maintenanceService.page(page, wrapper));
    }

    @PostMapping("/maintenance")
    public Result<Boolean> saveMaintenance(@RequestBody MaintenanceRecord record) {
        return Result.success(maintenanceService.save(record));
    }

    // ==================== 销售记录 (SalesRecord) ====================

    @GetMapping("/sales/list")
    public Result<Page<SalesRecord>> listSales(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) String batteryId) {
        Page<SalesRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SalesRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(batteryId), SalesRecord::getBatteryId, batteryId);
        wrapper.orderByDesc(SalesRecord::getSalesDate);
        return Result.success(salesService.page(page, wrapper));
    }

    // ==================== 车辆信息 (VehicleInfo) ====================

    @GetMapping("/vehicle/list")
    public Result<Page<VehicleInfo>> listVehicle(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 @RequestParam(required = false) String vin,
                                                 @RequestParam(required = false) String batteryId) {
        Page<VehicleInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<VehicleInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(vin), VehicleInfo::getVin, vin);
        wrapper.like(StringUtils.hasText(batteryId), VehicleInfo::getBatteryId, batteryId);
        wrapper.orderByDesc(VehicleInfo::getBindTime);
        return Result.success(vehicleService.page(page, wrapper));
    }

    @PostMapping("/vehicle")
    public Result<Boolean> saveVehicle(@RequestBody VehicleInfo vehicle) {
        return Result.success(vehicleService.save(vehicle));
    }

    @PutMapping("/vehicle")
    public Result<Boolean> updateVehicle(@RequestBody VehicleInfo vehicle) {
        return Result.success(vehicleService.updateById(vehicle));
    }

    @lombok.Data
    public static class RecyclingApplyDto {
        private String batteryId;
        private String reason;
        private String appearance;
        private String suggestion;
    }

    @lombok.Data
    public static class RecyclingAuditDto {
        private Long appraisalId;
        private Integer status;
        private String auditOpinion;
    }

    @lombok.Data
    public static class RecyclingValuationCalcDto {
        private Long appraisalId;
        private BigDecimal preliminaryValue;
        private String valuationBasis;
    }

    @lombok.Data
    public static class RecyclingValuationConfirmDto {
        private Long appraisalId;
        private BigDecimal finalValue;
    }
}
