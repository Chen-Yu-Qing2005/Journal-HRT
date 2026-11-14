# Journal-HRT 11.9-hrt.1

这是一个基于 Journal Android Multilingual 11.9 的 GPLv3 修改版，内置 HRT 常用药物扩展。

## 安装

- Android 8.0 及以上
- 包名：`io.github.chenyuqing2005.journal`
- 可与原 Journal 同时安装
- 直接安装 `Journal-HRT-11.9-hrt.1.apk`

首次启动会自动安装内置扩展包，无需手动导入。

## 内置扩展

包含 13 个 HRT 相关物质和植入剂：

- 戊酸雌二醇
- 雌二醇舌下片
- 雌二醇凝胶
- 雌二醇贴片
- 醋酸环丙孕酮
- 比卡鲁胺
- 黄体酮
- 醋酸甲羟孕酮
- 地屈孕酮
- 螺内酯
- 戈舍瑞林 3.6 mg 植入剂
- 戈舍瑞林 10.8 mg 植入剂
- 组氨瑞林 50 mg 植入剂

扩展内容包含中文/英文说明、安全使用、不良反应、剂量阈值、化学信息和公开来源。

完整药物、途径、参考 Cmax、Tmax 和半衰期表见 `PHARMACOKINETIC_PARAMETERS.md`。

## 本版功能

- 血药浓度估算与拖动时间同步
- `pg/mL`、`ng/mL`、`mg/mL`、`mg/L` 单位切换
- 同一种物质的多次给药叠加为一个总估算值
- 多种物质时浓度明细默认收起
- 按单位限制小数位，极低浓度自动隐藏
- 血药浓度阈值通知，默认峰值的 10%，可自定义
- AlarmManager 定时提醒，WorkManager 作为后台兜底

## 重要说明

血药浓度是基于公开参考 `Cmax`、`Tmax` 和半衰期的近似模型，不是个体实测值。剂量线性缩放不能覆盖吸收、首过代谢、制剂、体重、肝肾功能和个体差异。

本应用及扩展包不构成医疗建议、诊断、处方或剂量推荐。处方药的使用、调整和停药应遵循专业医疗人员意见。

## 通知

Android 13 及以上需要授予通知权限。部分厂商系统还需要允许自启动、后台运行、通知横幅和精确闹钟；系统“强行停止”应用后，任何 Android 定时方案都会被暂停。

## 许可证与来源

- 上游：Journal Android Multilingual，GPLv3-only
- 修改维护：Chen Yuqing
- 完整许可证见源码中的 `COPYING`
- 源码：https://github.com/Chen-Yu-Qing2005/Journal-HRT

