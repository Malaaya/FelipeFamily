package com.duoc.felipesfamily

import com.duoc.felipesfamily.util.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun `required devuelve el mensaje cuando esta vacio y null cuando tiene texto`() {
        assertEquals("Obligatorio", Validators.required("", "Obligatorio"))
        assertEquals("Obligatorio", Validators.required("   ", "Obligatorio"))
        assertNull(Validators.required("Trigo", "Obligatorio"))
    }

    @Test
    fun `surface acepta decimales positivos y rechaza cero negativos o letras`() {
        assertNull(Validators.surface("15.5"))
        assertNull(Validators.surface("10,5"))
        assertNotNull(Validators.surface("0"))
        assertNotNull(Validators.surface("-4"))
        assertNotNull(Validators.surface("abc"))
    }

    @Test
    fun `period valida formato dd-mm y detecta traslape de fechas`() {
        assertNull(Validators.period("20/11 - 30/11"))
        assertNotNull(Validators.period("30/11 - 20/11"))
        assertNotNull(Validators.period("20-11-2026"))
        assertTrue(Validators.overlaps("20/11 - 25/11", "15/11 - 22/11"))
        assertFalse(Validators.overlaps("20/11 - 25/11", "26/11 - 30/11"))
    }

    @Test
    fun `intInRange valida cobertura y experiencia dentro de los limites`() {
        assertNull(Validators.intInRange("50", 1, 500))
        assertNotNull(Validators.intInRange("0", 1, 500))
        assertNotNull(Validators.intInRange("600", 1, 500))
    }
}