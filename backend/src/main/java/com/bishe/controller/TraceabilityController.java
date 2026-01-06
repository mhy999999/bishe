package com.bishe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.common.Result;
import com.bishe.entity.*;
import com.bishe.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.regression.RandomForest;

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
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int AI_RECYCLING_MODEL_ID = 1;
    private static final java.util.List<String> AI_RECYCLING_FEATURES = java.util.List.of(
            "capacity",
            "voltage",
            "capacityVoltage",
            "maintenanceCount",
            "batteryStatus"
    );

    @lombok.Data
    private static class AiRecyclingModel {
        private String type;
        private java.util.List<String> features;
        private String modelBase64;
        private int samples;
        private String trainedAt;
        private java.util.Map<String, Object> params;
    }

    private volatile String cachedAiRecyclingModelJson;
    private volatile RandomForest cachedAiRecyclingModel;

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
        return Result.success(transferService.save(record));
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
        if (!hasAnyRole(userId, "admin", "recycler", "maintainer", "maintenance")) {
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
        String reason = dto.getReason();
        if (!StringUtils.hasText(reason) || reason.trim().length() < 50) {
            return Result.error(400, "回收原因说明不少于50字");
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

        String applyTxHash = chainService.submitTransaction(
                "submitRecyclingApply",
                cn.hutool.json.JSONUtil.toJsonStr(java.util.Map.of(
                        "appraisalId", appraisal.getAppraisalId(),
                        "batteryId", appraisal.getBatteryId(),
                        "applyUser", applyUser,
                        "applyTime", appraisal.getApplyTime(),
                        "applyReason", appraisal.getApplyReason(),
                        "snapshotVoltage", appraisal.getSnapshotVoltage(),
                        "snapshotCapacity", appraisal.getSnapshotCapacity(),
                        "snapshotBatteryStatus", appraisal.getSnapshotBatteryStatus()
                ))
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

            chainService.submitTransaction(
                    "uploadRecyclingPhoto",
                    cn.hutool.json.JSONUtil.toJsonStr(java.util.Map.of(
                            "appraisalId", appraisalId,
                            "batteryId", appraisal.getBatteryId(),
                            "operator", getCurrentUserDisplayName(request),
                            "url", url,
                            "time", LocalDateTime.now()
                    ))
            );
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

            chainService.submitTransaction(
                    "uploadRecyclingReport",
                    cn.hutool.json.JSONUtil.toJsonStr(java.util.Map.of(
                            "appraisalId", appraisalId,
                            "batteryId", appraisal.getBatteryId(),
                            "operator", getCurrentUserDisplayName(request),
                            "url", url,
                            "time", LocalDateTime.now()
                    ))
            );
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
        LambdaQueryWrapper<MaintenanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(batteryId), MaintenanceRecord::getBatteryId, batteryId);
        wrapper.orderByDesc(MaintenanceRecord::getCreateTime);
        List<MaintenanceRecord> history = maintenanceService.list(wrapper);
        String historyJson = cn.hutool.json.JSONUtil.toJsonStr(history == null ? java.util.Collections.emptyList() : history);

        BigDecimal capacity = appraisal.getSnapshotCapacity();
        BigDecimal voltage = appraisal.getSnapshotVoltage();
        if (capacity == null || voltage == null) {
            BatteryInfo battery = StringUtils.hasText(batteryId) ? batteryInfoService.getById(batteryId) : null;
            if (battery != null) {
                if (capacity == null) {
                    capacity = battery.getCapacity();
                }
                if (voltage == null) {
                    voltage = battery.getVoltage();
                }
            }
        }
        if (capacity == null || voltage == null) {
            return Result.error(400, "电池关键指标缺失，无法估值");
        }

        int maintenanceCount = history == null ? 0 : history.size();
        Integer batteryStatus = appraisal.getSnapshotBatteryStatus();
        if (batteryStatus == null) {
            BatteryInfo battery = StringUtils.hasText(batteryId) ? batteryInfoService.getById(batteryId) : null;
            batteryStatus = battery != null ? battery.getStatus() : null;
        }
        if (batteryStatus == null) {
            batteryStatus = 0;
        }

        AiRecyclingModel model = ensureAiRecyclingModel();
        double cap = capacity.doubleValue();
        double vol = voltage.doubleValue();
        double capVol = cap * vol;
        double[] x = new double[]{cap, vol, capVol, maintenanceCount, batteryStatus};
        Double predicted = predict(model, x);
        BigDecimal preliminary;
        String basis;
        if (predicted == null || !Double.isFinite(predicted) || predicted < 0) {
            BigDecimal base = capacity.multiply(voltage).multiply(new BigDecimal("0.60"));
            BigDecimal penalty = new BigDecimal(maintenanceCount).multiply(new BigDecimal("20"));
            BigDecimal fallback = base.subtract(penalty);
            if (fallback.compareTo(BigDecimal.ZERO) < 0) {
                fallback = BigDecimal.ZERO;
            }
            preliminary = fallback.setScale(2, java.math.RoundingMode.HALF_UP);
            basis = "ai_model_unavailable_fallback; capacity*voltage*0.60 - maintenanceCount*20; capacity=" + capacity + ", voltage=" + voltage + ", maintenanceCount=" + maintenanceCount;
        } else {
            preliminary = new BigDecimal(predicted).setScale(2, java.math.RoundingMode.HALF_UP);
            basis = "ai_random_forest_local; features=" + AI_RECYCLING_FEATURES + "; capacity=" + capacity + ", voltage=" + voltage + ", capacityVoltage=" + new BigDecimal(capVol).setScale(4, java.math.RoundingMode.HALF_UP) + ", maintenanceCount=" + maintenanceCount + ", batteryStatus=" + batteryStatus + "; samples=" + (model == null ? 0 : model.getSamples());
        }

        RecyclingAppraisal update = new RecyclingAppraisal();
        update.setAppraisalId(appraisal.getAppraisalId());
        update.setPreliminaryValue(preliminary);
        update.setResidualValue(preliminary);
        update.setMaintenanceHistoryJson(historyJson);
        update.setValuationBasis(basis);
        update.setUpdateTime(LocalDateTime.now());
        recyclingService.updateById(update);

        chainService.submitTransaction(
                "calcRecyclingValuation",
                cn.hutool.json.JSONUtil.toJsonStr(java.util.Map.of(
                        "appraisalId", appraisal.getAppraisalId(),
                        "batteryId", appraisal.getBatteryId(),
                        "preliminaryValue", preliminary,
                        "valuationBasis", basis,
                        "operator", getCurrentUserDisplayName(request),
                        "time", LocalDateTime.now()
                ))
        );

        RecyclingAppraisal updated = recyclingService.getById(appraisal.getAppraisalId());
        return Result.success(updated);
    }

    @PostMapping("/recycling/valuation/ai/train")
    @Transactional(rollbackFor = Exception.class)
    public Result<java.util.Map<String, Object>> trainRecyclingValuationAi(@RequestBody AiTrainDto dto,
                                                                           HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (!hasAnyRole(userId, "admin", "recycler")) {
            return forbidden();
        }

        int ntrees = dto != null && dto.getNtrees() != null ? dto.getNtrees() : (dto != null && dto.getEpochs() != null ? dto.getEpochs() : 300);
        int maxDepth = dto != null && dto.getMaxDepth() != null ? dto.getMaxDepth() : 18;
        int maxNodes = dto != null && dto.getMaxNodes() != null ? dto.getMaxNodes() : 0;
        int nodeSize = dto != null && dto.getNodeSize() != null ? dto.getNodeSize() : 5;
        double subsample = dto != null && dto.getSubsample() != null ? dto.getSubsample() : 1.0;
        int mtry = dto != null && dto.getMtry() != null ? dto.getMtry() : 0;

        if (ntrees < 50 || ntrees > 3000) {
            return Result.error(400, "ntrees范围应在50~3000");
        }
        if (maxDepth < 2 || maxDepth > 60) {
            return Result.error(400, "maxDepth范围应在2~60");
        }
        if (maxNodes < 0 || maxNodes > 200000) {
            return Result.error(400, "maxNodes范围应在0~200000");
        }
        if (nodeSize < 1 || nodeSize > 200) {
            return Result.error(400, "nodeSize范围应在1~200");
        }
        if (!(subsample > 0 && subsample <= 1)) {
            return Result.error(400, "subsample范围应在(0,1]");
        }
        if (mtry < 0 || mtry > AI_RECYCLING_FEATURES.size()) {
            return Result.error(400, "mtry范围应在0~特征数");
        }

        java.util.List<RecyclingAppraisal> rows = recyclingService.list(new LambdaQueryWrapper<RecyclingAppraisal>()
                .eq(RecyclingAppraisal::getStatus, 3)
                .isNotNull(RecyclingAppraisal::getFinalValue)
                .orderByDesc(RecyclingAppraisal::getRecycleTime));

        java.util.List<double[]> xs = new java.util.ArrayList<>();
        java.util.List<Double> ys = new java.util.ArrayList<>();
        for (RecyclingAppraisal r : rows) {
            if (r == null || r.getFinalValue() == null) {
                continue;
            }
            String bid = r.getBatteryId();
            BigDecimal c = r.getSnapshotCapacity();
            BigDecimal v = r.getSnapshotVoltage();
            Integer s = r.getSnapshotBatteryStatus();
            BatteryInfo b = null;
            if ((c == null || v == null || s == null) && StringUtils.hasText(bid)) {
                b = batteryInfoService.getById(bid);
            }
            if (c == null && b != null) {
                c = b.getCapacity();
            }
            if (v == null && b != null) {
                v = b.getVoltage();
            }
            if (s == null && b != null) {
                s = b.getStatus();
            }
            if (c == null || v == null) {
                continue;
            }
            if (s == null) {
                s = 0;
            }
            long mc = 0;
            if (StringUtils.hasText(bid)) {
                mc = maintenanceService.count(new LambdaQueryWrapper<MaintenanceRecord>().eq(MaintenanceRecord::getBatteryId, bid));
            }
            double cap = c.doubleValue();
            double vol = v.doubleValue();
            double capVol = cap * vol;
            xs.add(new double[]{cap, vol, capVol, mc, s});
            ys.add(r.getFinalValue().doubleValue());
        }

        if (xs.size() < 5) {
            return Result.error(400, "可训练样本不足（至少需要5条已完成回收记录）");
        }

        int m = xs.size();
        int n = AI_RECYCLING_FEATURES.size();
        double[][] X = xs.toArray(new double[0][]);
        double[][] ycol = new double[m][1];
        for (int i = 0; i < m; i++) {
            ycol[i][0] = ys.get(i);
        }
        String[] cols = AI_RECYCLING_FEATURES.toArray(new String[0]);
        DataFrame df = DataFrame.of(X, cols).merge(DataFrame.of(ycol, "y"));
        RandomForest rf = RandomForest.fit(Formula.lhs("y"), df, ntrees, mtry, maxDepth, maxNodes, nodeSize, subsample);

        String base64 = serializeToBase64(rf);
        if (!StringUtils.hasText(base64)) {
            return Result.error(500, "模型训练完成但序列化失败，请检查依赖与运行环境");
        }
        AiRecyclingModel model = new AiRecyclingModel();
        model.setType("smile_rf");
        model.setFeatures(AI_RECYCLING_FEATURES);
        model.setModelBase64(base64);
        model.setSamples(m);
        model.setTrainedAt(LocalDateTime.now().toString());
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("ntrees", ntrees);
        params.put("mtry", mtry);
        params.put("maxDepth", maxDepth);
        params.put("maxNodes", maxNodes);
        params.put("nodeSize", nodeSize);
        params.put("subsample", subsample);
        model.setParams(params);
        if (!saveAiRecyclingModel(model)) {
            return Result.error(500, "模型已训练但保存失败：请确认 ai_recycling_model 表存在且可写");
        }

        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("samples", m);
        resp.put("features", AI_RECYCLING_FEATURES);
        resp.put("trainedAt", model.getTrainedAt());
        resp.put("type", model.getType());
        resp.put("params", model.getParams());
        return Result.success(resp);
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
        String receiptHash = chainService.submitTransaction(
                "confirmRecyclingPrice",
                cn.hutool.json.JSONUtil.toJsonStr(java.util.Map.of(
                        "appraisalId", appraisal.getAppraisalId(),
                        "batteryId", appraisal.getBatteryId(),
                        "recycleNo", appraisal.getRecycleNo(),
                        "preliminaryValue", appraisal.getPreliminaryValue(),
                        "finalValue", dto.getFinalValue(),
                        "priceReviewer", reviewer,
                        "time", now
                ))
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

    private AiRecyclingModel ensureAiRecyclingModel() {
        AiRecyclingModel model = loadAiRecyclingModel();
        if (model != null && StringUtils.hasText(model.getModelBase64())) {
            return model;
        }
        return null;
    }

    private Double predict(AiRecyclingModel model, double[] raw) {
        if (model == null || raw == null || raw.length < AI_RECYCLING_FEATURES.size()) {
            return null;
        }
        if (!"smile_rf".equalsIgnoreCase(model.getType())) {
            return null;
        }

        String json = cn.hutool.json.JSONUtil.toJsonStr(model);
        RandomForest rf;
        if (StringUtils.hasText(cachedAiRecyclingModelJson) && cachedAiRecyclingModel != null && cachedAiRecyclingModelJson.equals(json)) {
            rf = cachedAiRecyclingModel;
        } else {
            rf = deserializeFromBase64(model.getModelBase64());
            if (rf == null) {
                return null;
            }
            cachedAiRecyclingModelJson = json;
            cachedAiRecyclingModel = rf;
        }

        String[] cols = AI_RECYCLING_FEATURES.toArray(new String[0]);
        DataFrame df = DataFrame.of(new double[][]{raw}, cols);
        double[] out = rf.predict(df);
        if (out == null || out.length == 0) {
            return null;
        }
        return out[0];
    }

    private void ensureAiRecyclingModelTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS ai_recycling_model (" +
                            "id INT PRIMARY KEY," +
                            "model_json LONGTEXT," +
                            "train_samples INT," +
                            "update_time DATETIME" +
                            ")"
            );
        } catch (Exception ignored) {
        }
    }

    private AiRecyclingModel loadAiRecyclingModel() {
        try {
            ensureAiRecyclingModelTable();
            java.util.List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT model_json, train_samples FROM ai_recycling_model WHERE id = ?", AI_RECYCLING_MODEL_ID);
            if (rows == null || rows.isEmpty()) {
                return null;
            }
            Object mj = rows.get(0).get("model_json");
            String modelJson = mj == null ? null : String.valueOf(mj);
            if (!StringUtils.hasText(modelJson)) {
                return null;
            }
            cn.hutool.json.JSONObject obj = cn.hutool.json.JSONUtil.parseObj(modelJson);
            AiRecyclingModel model = new AiRecyclingModel();
            model.setType(obj.getStr("type"));
            model.setFeatures(obj.getJSONArray("features") != null ? obj.getJSONArray("features").toList(String.class) : AI_RECYCLING_FEATURES);
            model.setModelBase64(obj.getStr("modelBase64"));
            java.util.Map<String, Object> params = null;
            try {
                Object p = obj.get("params");
                if (p instanceof cn.hutool.json.JSONObject pj) {
                    params = new java.util.HashMap<>();
                    for (java.util.Map.Entry<String, Object> e : pj.entrySet()) {
                        params.put(e.getKey(), e.getValue());
                    }
                } else if (p instanceof java.util.Map<?, ?> pm) {
                    params = new java.util.HashMap<>();
                    for (java.util.Map.Entry<?, ?> e : pm.entrySet()) {
                        String k = e.getKey() == null ? null : String.valueOf(e.getKey());
                        if (StringUtils.hasText(k)) {
                            params.put(k, e.getValue());
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            model.setParams(params);
            Object ts = rows.get(0).get("train_samples");
            int samples = 0;
            if (ts instanceof Number num) {
                samples = num.intValue();
            } else if (ts != null) {
                try {
                    samples = Integer.parseInt(String.valueOf(ts));
                } catch (Exception ignored) {
                }
            }
            model.setSamples(samples);
            model.setTrainedAt(obj.getStr("trainedAt"));
            return model;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean saveAiRecyclingModel(AiRecyclingModel model) {
        if (model == null) {
            return false;
        }
        ensureAiRecyclingModelTable();
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", model.getType());
        payload.put("features", model.getFeatures() == null ? AI_RECYCLING_FEATURES : model.getFeatures());
        payload.put("modelBase64", model.getModelBase64());
        payload.put("samples", model.getSamples());
        payload.put("trainedAt", model.getTrainedAt());
        payload.put("params", model.getParams());
        String json = cn.hutool.json.JSONUtil.toJsonStr(payload);
        int samples = model.getSamples();
        try {
            int updated = jdbcTemplate.update(
                    "INSERT INTO ai_recycling_model(id, model_json, train_samples, update_time) VALUES (?, ?, ?, NOW()) " +
                            "ON DUPLICATE KEY UPDATE model_json=VALUES(model_json), train_samples=VALUES(train_samples), update_time=VALUES(update_time)",
                    AI_RECYCLING_MODEL_ID, json, samples
            );
            return updated > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String serializeToBase64(Object obj) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos)) {
                oos.writeObject(obj);
            }
            return java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    private RandomForest deserializeFromBase64(String base64) {
        if (!StringUtils.hasText(base64)) {
            return null;
        }
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(base64);
            try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes))) {
                Object obj = ois.readObject();
                if (obj instanceof RandomForest rf) {
                    return rf;
                }
                return null;
            }
        } catch (Exception e) {
            return null;
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
    public static class AiTrainDto {
        private Integer epochs;
        private Double lr;
        private Double l2;

        private Integer ntrees;
        private Integer mtry;
        private Integer maxDepth;
        private Integer maxNodes;
        private Integer nodeSize;
        private Double subsample;
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
    }

    @lombok.Data
    public static class RecyclingValuationConfirmDto {
        private Long appraisalId;
        private BigDecimal finalValue;
    }
}
