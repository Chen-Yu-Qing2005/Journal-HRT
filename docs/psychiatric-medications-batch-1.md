# Psychiatric medications extension: batch 1

The built-in `psychiatric_medications` extension starts with six SSRIs:

- Escitalopram
- Sertraline
- Citalopram
- Paroxetine
- Fluvoxamine
- Fluoxetine

## Clinical scope

The timeline records medicine exposure. It does not predict antidepressant response, which
commonly takes weeks, and it is not therapeutic drug monitoring.

The oral `RoaDose` entries use `isPharmaceutical: true`, which disables the recreational
volumetric-dosing prompt for milligram doses below 15 mg.

## Concentration models

- Sertraline: labeled adult steady-state reference at 200 mg/day.
- Paroxetine: labeled steady-state reference at 30 mg/day.
- Fluvoxamine: extended-release capsule reference at 100 mg/day.
- Escitalopram and citalopram: Tmax and half-life are recorded, but a simplified concentration
  curve is withheld because the selected label did not provide a suitable reference Cmax.
- Fluoxetine: a simple parent-drug curve is withheld because active norfluoxetine and nonlinear,
  prolonged accumulation make it misleading.

## Safety

Each record includes suicidal-thought warning, serotonin syndrome, discontinuation effects,
bleeding, hyponatremia, mania, glaucoma, sexual dysfunction, and drug-specific contraindications.
Citalopram additionally records dose-dependent QT risk and the reduced maximum for older adults,
hepatic impairment, CYP2C19 poor metabolizers, and CYP2C19 inhibitors.
