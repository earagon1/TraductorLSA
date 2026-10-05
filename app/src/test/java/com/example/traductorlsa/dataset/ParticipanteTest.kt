package com.example.traductorlsa.dataset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParticipanteTest {

    @Test
    fun `la misma cuenta da el mismo id en cualquier dispositivo`() {
        assertEquals(Participante.idSeudonimo("user_2abc"), Participante.idSeudonimo("user_2abc"))
    }

    @Test
    fun `cuentas distintas dan ids distintos`() {
        assertNotEquals(Participante.idSeudonimo("user_2abc"), Participante.idSeudonimo("user_2abd"))
    }

    @Test
    fun `el id no contiene la cuenta`() {
        val id = Participante.idSeudonimo("user_2abcdefgh")!!
        assertFalse(id.contains("user_"))
        assertFalse(id.contains("2abcdefgh"))
    }

    @Test
    fun `valor fijo para detectar un cambio accidental de la derivacion`() {
        // Si este valor cambia, los exports viejos y nuevos dejan de cruzarse por
        // persona. Calculado aparte, en Python:
        // "p1_" + sha256(b"senar-participante-v1:user_2abc").hexdigest()[:16]
        assertEquals("p1_34a397c8c04e7f25", Participante.idSeudonimo("user_2abc"))
    }

    @Test
    fun `sin cuenta no hay id`() {
        assertNull(Participante.idSeudonimo(null))
        assertNull(Participante.idSeudonimo(""))
        assertNull(Participante.idSeudonimo("   "))
    }

    @Test
    fun `reconoce un seudonimo y rechaza un id de cuenta crudo`() {
        assertTrue(Participante.esSeudonimo(Participante.idSeudonimo("user_x")))
        assertFalse(Participante.esSeudonimo("user_2abc"))
        assertFalse(Participante.esSeudonimo(null))
    }
}
