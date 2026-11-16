# 精神科药物文献与药代参数审核

审核日期：2026-11-17

审核范围：精神科扩展包中的 26 个新增药物条目。

## 审核方法

- 美国上市药物使用 openFDA 药品标签接口核对具体 SPL，并在 DailyMed 保留具体 `setid` 页面。
- Agomelatine 使用 EMA `Valdoxan` 产品信息 PDF。
- Amisulpride 使用英国 eMC 的 Amisulpride 100 mg Tablets SmPC。
- 重点核对 Tmax、峰浓度、消除半衰期和活性代谢物说明。
- 只记录来源能够直接支持的值。来源未提供或不同制剂差异过大时保留 `not_reported`，不使用经验值补全。

## 审核结论

- 26 个条目均已关联到具体药物标签或产品信息页面。
- 审核时 26 个主要来源链接均可正常访问。
- 已填写的 Tmax 和半衰期未发现与所引标签直接冲突。
- 对半衰期受代谢酶表型、肾功能、肝功能或合并用药显著影响的情况，保留范围或限制说明。
- 部分药物只有相对暴露模型，没有可用于绝对浓度缩放的参考 Cmax。
- Amisulpride、Hydroxyzine 和 Paliperidone 当前未建立简化浓度模型，因此不会伪造曲线。

## 逐药结果

| 药物 | 主要来源 | 标签支持的关键参数 | 审核状态 |
|---|---|---|---|
| 阿戈美拉汀 Agomelatine | EMA Valdoxan PI | Tmax 约 1–2 h；终末半衰期约 1–2 h | 已核对 |
| 氨磺必利 Amisulpride | UK eMC SmPC | 50 mg 后双峰约 1 h 与 3–4 h；Cmax 约 39 与 54 ng/mL；半衰期约 12 h | 来源核对；未建立单峰曲线 |
| 阿立哌唑 Aripiprazole | DailyMed | Tmax 3–5 h；母药半衰期约 75 h；活性代谢物约 94 h | 来源核对；当前模型未填数值 |
| 丁螺环酮 Buspirone | DailyMed | 峰浓度约 40–90 min；半衰期约 2–3 h | 来源核对；当前模型未填数值 |
| 西酞普兰 Citalopram | DailyMed | Tmax 约 3–5 h；半衰期约 30–40 h | 已核对 |
| 氯氮平 Clozapine | DailyMed | 稳态 Tmax 中位约 2.5 h、范围 1–6 h；稳态半衰期均值约 12 h、范围 4–66 h | 来源核对；当前模型未填数值 |
| 达利雷生 Daridorexant | DailyMed | Tmax 1–2 h；终末半衰期约 8 h | 已核对 |
| 去甲文拉法辛 Desvenlafaxine | DailyMed | 标签可确认主要代谢与排泄；当前扩展未写入 Tmax/半衰期 | 来源有效；数值未录入 |
| 度洛西汀 Duloxetine | DailyMed | Tlag 中位约 2 h；Tmax 约 6 h；半衰期约 12 h（8–17 h） | 来源核对；当前模型未填数值 |
| 艾司西酞普兰 Escitalopram | DailyMed | Tmax 约 4–6 h；半衰期约 27–32 h | 已核对 |
| 氟西汀 Fluoxetine | DailyMed | Tmax 6–8 h；母药半衰期 1–3 d，长期给药 4–6 d；Norfluoxetine 4–16 d | 已核对；当前仅估算母药 |
| 氟伏沙明 Fluvoxamine | DailyMed | 100 mg 缓释 Cmax 约 47 ng/mL；Tmax 约 6–9 h；半衰期 13.6–25.9 h | 已核对 |
| 羟嗪 Hydroxyzine | DailyMed | 当前标签没有可供统一缩放的单组 PK 模型 | 来源有效；未建立曲线 |
| 拉莫三嗪 Lamotrigine | DailyMed | Tmax 约 1.4–4.8 h；半衰期受丙戊酸、酶诱导剂及肾功能显著影响 | 来源核对；不建立通用曲线 |
| 仑博雷生 Lemborexant | DailyMed | Tmax 1–3 h；5 mg/10 mg 有效半衰期约 17/19 h | 已核对 |
| 鲁拉西酮 Lurasidone | DailyMed | Tmax 1–3 h；40 mg 后平均半衰期约 18 h | 来源核对；当前模型未填数值 |
| 帕利哌酮 Paliperidone | DailyMed | 缓释片 Tmax 约 24 h；终末半衰期约 23 h | 来源核对；当前未建立曲线 |
| 帕罗西汀 Paroxetine | DailyMed | 30 mg/d 稳态 Cmax 约 61.7 ng/mL；Tmax 约 4–7 h；半衰期约 21 h（14–30 h） | 已核对 |
| 雷美替胺 Ramelteon | DailyMed | 母药 Tmax 约 0.5–1.5 h；半衰期约 1–2.6 h | 已核对 |
| 舍曲林 Sertraline | DailyMed | 200 mg/d 稳态 Cmax 约 142 ng/mL；Tmax 4.5–8.4 h；半衰期约 26–28 h | 已核对 |
| 苏沃雷生 Suvorexant | DailyMed | Tmax 约 1–3 h；半衰期约 12–13 h | 已核对 |
| 他司美琼 Tasimelteon | DailyMed | Tmax 0.5–3 h；母药半衰期约 1.3 h | 已核对 |
| 曲唑酮 Trazodone | DailyMed | 空腹峰浓度约 1 h，餐后约 2 h；当前标签未给出可用于统一曲线的明确半衰期 | 来源有效；数值不完整 |
| 文拉法辛 Venlafaxine | DailyMed | Tmax 约 5–9 h；母药半衰期约 3–7 h；ODV 约 11 h | 已核对；当前仅估算母药 |
| 沃替西汀 Vortioxetine | DailyMed | Tmax 7–11 h；终末半衰期约 66 h | 来源核对；当前模型未填数值 |
| 齐拉西酮 Ziprasidone | DailyMed | Tmax 约 6–8 h；终末半衰期约 7 h | 来源核对；当前模型未填数值 |

## 发布边界

- 本审核确认的是“扩展数据与所引官方标签是否一致”，不等同于独立临床证据综述。
- 不同制剂、适应证、年龄、肝肾功能和 CYP 表型可能改变参数。
- 未填写的数值不会由模型猜测；相关药物仅使用明确的相对时间模型或保持 `not_reported`。
