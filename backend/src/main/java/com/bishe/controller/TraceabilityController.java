package com.bishe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bishe.common.Result;
import com.bishe.entity.*;
import com.bishe.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

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
                                                    @RequestParam(required = false) String txHash) {
        Page<ChainTransaction> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ChainTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(txHash), ChainTransaction::getTxHash, txHash);
        wrapper.orderByDesc(ChainTransaction::getCreateTime);
        return Result.success(chainService.page(page, wrapper));
    }

    // ==================== 回收评估 (RecyclingAppraisal) ====================

    @GetMapping("/recycling/list")
    public Result<Page<RecyclingAppraisal>> listRecycling(@RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                                          @RequestParam(required = false) String batteryId) {
        Page<RecyclingAppraisal> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<RecyclingAppraisal> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(batteryId), RecyclingAppraisal::getBatteryId, batteryId);
        return Result.success(recyclingService.page(page, wrapper));
    }

    @PostMapping("/recycling")
    public Result<Boolean> saveRecycling(@RequestBody RecyclingAppraisal appraisal) {
        return Result.success(recyclingService.save(appraisal));
    }

    @PutMapping("/recycling")
    public Result<Boolean> updateRecycling(@RequestBody RecyclingAppraisal appraisal) {
        return Result.success(recyclingService.updateById(appraisal));
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
                                                 @RequestParam(required = false) String vin) {
        Page<VehicleInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<VehicleInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(vin), VehicleInfo::getVin, vin);
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
}
