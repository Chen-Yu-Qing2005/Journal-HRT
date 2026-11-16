/*
 * Copyright (c) 2022. Isaak Hanimann.
 * This file is part of PsychonautWiki Journal.
 *
 * PsychonautWiki Journal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * PsychonautWiki Journal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PsychonautWiki Journal.  If not, see https://www.gnu.org/licenses/gpl-3.0.en.html.
 */

package com.isaakhanimann.journal

import com.isaakhanimann.journal.data.substances.ReleaseForm
import com.isaakhanimann.journal.data.substances.parse.SubstanceParser
import com.isaakhanimann.journal.data.substances.parse.getOptionalBoolean
import com.isaakhanimann.journal.data.substances.parse.getOptionalDouble
import com.isaakhanimann.journal.data.substances.parse.getOptionalLong
import com.isaakhanimann.journal.data.substances.parse.getOptionalString
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TestParse {

    private val parser = SubstanceParser()

    @Test
    fun parsesConcentrationModelForRoute() {
        val substance = parser.parseSubstance(
            """
            {
              "name": "Concentration test",
              "concentrationModels": {
                "oral": {
                  "status": "reference",
                  "referenceDose": 1,
                  "referenceDoseUnits": "mg",
                  "cmax": 1,
                  "cmaxUnits": "ng/mL",
                  "cmaxMgPerL": 0.001
                }
              }
            }
            """.trimIndent()
        )!!

        val model = substance.getConcentrationModel(
            com.isaakhanimann.journal.data.substances.AdministrationRoute.ORAL
        )!!
        assertEquals(1.0, model.referenceDose!!, 0.0)
        assertEquals(0.001, model.cmaxMgPerL!!, 0.0)
    }

    @Test
    fun noCrash() {
        val substances = parser.parseSubstanceFile(string = "error")
        assertTrue(substances.substances.isEmpty())
    }

    @Test
    fun noCrashExtract() {
        val result = parser.extractSubstanceString(string = "error")
        assertTrue(result == null)
    }

    @Test
    fun testExtractSubstancesString() {
        val text = """
{
  "data": {
    "substances": [
      {
        "name": "Armodafinil",
        "roas": [
          {
            "name": "oral"
          }
        ]
      }
    ]
  }
}"""
        val result = parser.extractSubstanceString(string = text)
        assertTrue(result == "[{\"name\":\"Armodafinil\",\"roas\":[{\"name\":\"oral\"}]}]")
    }

    @Test
    fun parseSingleSubstance() {
        val substance = parser.parseSubstance(
            string = """
                {
                  "name": "Armodafinil",
                  "url": "https://example.com/armodafinil",
                  "metabolism": "Amide hydrolysis and oxidation.",
                  "metabolismSources": ["https://example.com/metabolism"],
                  "oralReleaseForms": ["IMMEDIATE_RELEASE", "EXTENDED_RELEASE", "future_form"],
                  "categories": ["stimulant"],
                  "roas": [
                    {
                      "name": "oral"
                    }
                  ]
                }
            """.trimIndent()
        )
        assertTrue(substance?.name == "Armodafinil")
        assertEquals("Amide hydrolysis and oxidation.", substance?.metabolism)
        assertEquals(listOf("https://example.com/metabolism"), substance?.metabolismSources)
        assertEquals(
            listOf(ReleaseForm.IMMEDIATE_RELEASE, ReleaseForm.EXTENDED_RELEASE),
            substance?.oralReleaseForms
        )
    }

    @Test
    fun parsesExtendedReleasePresetForOralRoute() {
        val substance = parser.parseSubstance(
            """
            {
              "name": "ER demo",
              "roas": [
                {
                  "name": "oral",
                  "dose": {
                    "units": "mg",
                    "commonMin": 5
                  },
                  "duration": {
                    "total": {
                      "min": 8,
                      "max": 12,
                      "units": "hours"
                    }
                  }
                }
              ],
              "extendedReleasePreset": {
                "initialMinutes": 60,
                "initialPercent": 32,
                "subsequentMinutes": 480,
                "formulation": "Synthetic ER tablet",
                "source": "Synthetic acceptance-test preset",
                "note": "Fictional data"
              }
            }
            """.trimIndent()
        )!!

        val preset = substance.getExtendedReleasePreset(
            com.isaakhanimann.journal.data.substances.AdministrationRoute.ORAL
        )!!
        assertEquals(60.0, preset.profile.initialMinutes, 0.0)
        assertEquals(32.0, preset.profile.initialPercent, 0.0)
        assertEquals(480.0, preset.profile.subsequentMinutes, 0.0)
        assertEquals("Synthetic ER tablet", preset.formulation)
        assertEquals("Synthetic acceptance-test preset", preset.source)
        assertNull(
            substance.getExtendedReleasePreset(
                com.isaakhanimann.journal.data.substances.AdministrationRoute.RECTAL
            )
        )
    }

    @Test
    fun groupsRelatedAdministrationMethodsAndHidesVariantEntries() {
        val file = parser.parseSubstanceFile(
            """
            {
              "substances": [
                {
                  "name": "Estradiol",
                  "roas": [
                    { "name": "oral" },
                    { "name": "intramuscular" }
                  ],
                  "administrationMethodGroup": {
                    "id": "estradiol-methods",
                    "title": "戊酸雌二醇",
                    "methods": [
                      {
                        "label": "口服片",
                        "substanceName": "Estradiol",
                        "route": "oral",
                        "activeIngredient": "戊酸雌二醇"
                      },
                      {
                        "label": "油剂注射",
                        "substanceName": "Estradiol",
                        "route": "intramuscular",
                        "activeIngredient": "戊酸雌二醇油剂"
                      },
                      {
                        "label": "凝胶",
                        "substanceName": "Estradiol gel",
                        "route": "transdermal",
                        "activeIngredient": "雌二醇"
                      }
                    ]
                  }
                },
                {
                  "name": "Estradiol gel",
                  "roas": [
                    { "name": "transdermal" }
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        val group = file.substancesMap["Estradiol"]!!.administrationMethodGroup!!
        assertEquals("戊酸雌二醇", group.title)
        assertEquals(3, group.methods.size)
        assertEquals("戊酸雌二醇油剂", group.methods[1].activeIngredient)
        assertTrue(file.visibleSubstances.any { it.name == "Estradiol" })
        assertTrue(file.visibleSubstances.none { it.name == "Estradiol gel" })
        assertTrue(file.substances.any { it.name == "Estradiol gel" })
    }

    @Test
    fun pharmaceuticalOralDoseDoesNotTriggerVolumetricDosingPrompt() {
        val substance = parser.parseSubstance(
            """
            {
              "name": "Escitalopram",
              "roas": [
                {
                  "name": "oral",
                  "dose": {
                    "units": "mg",
                    "lightMin": 5,
                    "commonMin": 10,
                    "strongMin": 20,
                    "isPharmaceutical": true
                  }
                }
              ]
            }
            """.trimIndent()
        )!!

        val dose = substance.getRoa(
            com.isaakhanimann.journal.data.substances.AdministrationRoute.ORAL
        )!!.roaDose!!
        assertTrue(dose.isPharmaceutical)
        assertTrue(!dose.shouldUseVolumetricDosing)
    }

    @Test
    fun parseCategoriesArray() {
        val categories = parser.parseCategories(
            """
                [
                  {
                    "name": "test",
                    "description": "desc",
                    "color": 1234
                  }
                ]
            """.trimIndent()
        )
        assertTrue(categories.size == 1)
        assertTrue(categories.first().name == "test")
    }

    @Test
    fun parserRejectsSubstanceWithMissingOrBlankName() {
        assertNull(parser.parseSubstance("""{"url": "https://example.com"}"""))
        assertNull(parser.parseSubstance("""{"name": ""}"""))
        assertNull(parser.parseSubstance("""{"name": "   "}"""))
    }

    @Test
    fun parserExtensionsStrictTypes() {
        val json = JSONObject(
            """
            {
                "validLong": 42,
                "stringLong": "100",
                "invalidLong": "not_a_number",
                "validBool": true,
                "stringBool": "true",
                "invalidBool": "maybe",
                "validString": "hello",
                "validNumberString": 123,
                "objectString": {"nested": "value"},
                "validDouble": 3.14,
                "invalidDouble": "not_double"
            }
            """.trimIndent()
        )

        // getOptionalLong
        assertEquals(42L, json.getOptionalLong("validLong"))
        assertEquals(100L, json.getOptionalLong("stringLong"))
        assertNull(json.getOptionalLong("invalidLong"))
        assertNull(json.getOptionalLong("missingKey"))

        // getOptionalBoolean
        assertEquals(true, json.getOptionalBoolean("validBool"))
        assertEquals(true, json.getOptionalBoolean("stringBool"))
        assertNull(json.getOptionalBoolean("invalidBool"))
        assertNull(json.getOptionalBoolean("missingKey"))

        // getOptionalString
        assertEquals("hello", json.getOptionalString("validString"))
        assertEquals("123", json.getOptionalString("validNumberString"))
        assertNull(json.getOptionalString("objectString"))
        assertNull(json.getOptionalString("missingKey"))

        // getOptionalDouble
        assertEquals(3.14, json.getOptionalDouble("validDouble")!!, 0.001)
        assertNull(json.getOptionalDouble("invalidDouble"))
        assertNull(json.getOptionalDouble("missingKey"))
    }
}
