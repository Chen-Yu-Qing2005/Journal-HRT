package com.isaakhanimann.journal.data.substances

import java.text.Normalizer
import java.util.Locale

/**
 * PRC regulatory classes sourced from the 2025 medicinal and non-medicinal
 * catalogues published by NMPA, MPS, and NHC.
 *
 * Only explicitly listed substances are returned. The three broad classes in
 * the non-medicinal catalogue (fentanyl analogues, synthetic cannabinoids, and
 * nitazenes) are the sole exceptions and are evaluated from their official
 * class definitions, never from generic drug categories.
 */
enum class RegulatorySchedule {
    CN_PSYCHOTROPIC_I,
    CN_PSYCHOTROPIC_II,
    CN_NARCOTIC,
    CN_NON_MEDICAL;

    companion object {
        private val cnNarcotics = normalizedNameSet(
            """
            codeine
            dihydrocodeine
            ethylmorphine
            fentanyl
            hydrocodone
            hydromorphone
            methadone
            morphine
            oliceridine
            opium
            oxycodone
            oxymorphone
            pethidine
            sufentanil
            tilidine
            """
        )

        private val cnPsychotropicI = normalizedNameSet(
            """
            amfepramone
            amphetamine
            buprenorphine
            bzp
            dextroamphetamine
            ghb
            hydrocodone
            ketamine
            mazindol
            methaqualone
            methylphenidate
            midazolam
            oxycodone
            secobarbital
            tapentadol
            triazolam
            """
        )

        private val cnPsychotropicII = normalizedNameSet(
            """
            allobarbital
            alprazolam
            amobarbital
            barbital
            bromazepam
            brotizolam
            buprenorphine
            butorphanol
            caffeine
            carisoprodol
            cathine
            chlordiazepoxide
            clobazam
            clonazepam
            clotiazepam
            cloxazolam
            codeine
            delorazepam
            dexfenfluramine
            dextromethorphan
            dezocine
            diazepam
            dimdazenil
            estazolam
            ethylloflazepate
            etizolam
            fenfluramine
            fludiazepam
            flunitrazepam
            flurazepam
            glutethimide
            hydrocodone
            loprazolam
            lorazepam
            lorcaserin
            lormetazepam
            medazepam
            meprobamate
            methylphenobarbital
            midazolam
            modafinil
            nalbuphine
            nimetazepam
            nitrazepam
            nordazepam
            oxazepam
            oxazolam
            oxycodone
            pentazocine
            pentobarbital
            perampanel
            phenobarbital
            phentermine
            pinazepam
            pipradrol
            prazepam
            remimazolam
            temazepam
            tetrazepam
            tramadol
            zaleplon
            zolpidem
            zopiclone
            zuranolone
            """
        )

        private val cnNonMedical = normalizedNameSet(
            """
            1cplsd
            1plsd
            1vlsd
            25cnbome
            2aminoindane
            2cb
            2cc
            2cd
            2ce
            2ch
            2ci
            2cp
            2ct2
            2ct4
            2ct7
            2fa
            2fea
            2fluorodeschloroketamine
            2fma
            2mmc
            3cmc
            3fa
            3fea
            3fma
            3fpm
            3hopcp
            3meomc
            3meopcp
            3meopcpy
            3mmc
            4acodmt
            4bmc
            4ca
            4cmc
            4emc
            4fa
            4fea
            4fluorobutyrfentanyl
            4fma
            4fmc
            4homipt
            4mec
            4meopcp
            4methylaminorex
            4mpd
            4mta
            5apb
            5apdb
            5fakb48
            5fpb22
            5mapb
            5meoalphamt
            5meodalt
            5meodipt
            5meodmt
            5meomipt
            6apb
            abchminaca
            abfubinaca
            acetylfentanyl
            acrylfentanyl
            ah7921
            alphaet
            alphamt
            alphapbp
            alphapvt
            am2201
            amineptine
            aminorex
            aphp
            apica
            apvp
            bezitramide
            bromazolam
            buphedrone
            butobarbital
            butylone
            camazepam
            cannabis
            cathinone
            clonazolam
            clonitazene
            clorazepate
            cocaine
            cyclobarbital
            deschloroketamine
            desomorphine
            desoxypipradrol
            det
            dextromoramide
            dextropropoxyphene
            diclazepam
            diphenidine
            dipipanone
            dmt
            dob
            doc
            doet
            dom
            ethaqualone
            ethcat
            ethchlorvynol
            ethylone
            ethylphenidate
            fencamfamin
            fenethylline
            flualprazolam
            flubromazepam
            flubromazolam
            furanylfentanyl
            halazepam
            heroin
            isomethadone
            jwh018
            jwh073
            ketazolam
            ketobemidone
            khat
            kratom
            lsd
            marinol
            mcpp
            mda
            mdai
            mdea
            mdma
            mdphp
            mdpv
            mefenorex
            mephedrone
            mescaline
            methallylescaline
            methamphetamine
            methcathinone
            methedrone
            methiopropamine
            methoxetamine
            methoxphenidine
            methylone
            methyprylon
            mexedrone
            mmda
            morpheridine
            mt45
            neb
            nethylhexedrone
            nicomorphine
            parafluorofentanyl
            pce
            pcp
            pemoline
            pentedrone
            pentylone
            phenazepam
            phenazocine
            phenmetrazine
            piritramide
            pma
            pmma
            propylhexedrine
            proscaline
            psilocin
            rolicyclidine
            sts135
            tetrahydrocannabinol
            tetrahydrofuranfentanyl
            u47700
            valerylfentanyl
            vinylbital
            """
        )

        fun fromName(name: String?): RegulatorySchedule? =
            entries.firstOrNull { it.name == name }

        fun forSubstanceName(
            substanceName: String,
            categories: List<String> = emptyList()
        ): List<RegulatorySchedule> {
            val normalized = normalizeName(substanceName)
            if (normalized.isEmpty()) return emptyList()
            val normalizedCategories = categories.map(::normalizeName).toSet()
            val isMedicinalNarcotic = normalized in cnNarcotics
            return buildList {
                if (isMedicinalNarcotic) add(CN_NARCOTIC)
                if (normalized in cnPsychotropicI) add(CN_PSYCHOTROPIC_I)
                if (normalized in cnPsychotropicII) add(CN_PSYCHOTROPIC_II)
                if (
                    normalized in cnNonMedical ||
                    (
                        !isMedicinalNarcotic &&
                            isNonMedicalDirectoryClass(normalized, normalizedCategories)
                        )
                ) {
                    add(CN_NON_MEDICAL)
                }
            }
        }

        private fun isNonMedicalDirectoryClass(
            normalizedName: String,
            categories: Set<String>
        ): Boolean {
            val isFentanylClass =
                normalizedName.contains("fentanyl") || normalizedName.contains("fentanil")
            val isNitazeneClass =
                normalizedName.contains("tazene") || normalizedName.contains("tazepyne")
            val isSyntheticCannabinoidClass =
                "cannabinoid" in categories &&
                    "researchchemical" in categories &&
                    normalizedName != "cannabidiol"
            return isFentanylClass || isNitazeneClass || isSyntheticCannabinoidClass
        }

        private fun normalizedNameSet(names: String): Set<String> =
            names.lineSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toSet()

        private fun normalizeName(name: String): String =
            Normalizer.normalize(name, Normalizer.Form.NFKC)
                .lowercase(Locale.ROOT)
                .replace("α", "alpha")
                .replace("β", "beta")
                .replace("γ", "gamma")
                .replace("δ", "delta")
                .replace("κ", "kappa")
                .replace(nonAlphanumeric, "")

        private val nonAlphanumeric = Regex("[^\\p{L}\\p{N}]+")
    }
}
