package com.example.smartmedicinebox.ui.viewmodel

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AiAssistantSafetyTest {
    @Test
    fun urgentSafetyMessage_detectsMalayEmergencyPhrase() {
        assertNotNull(AiAssistantViewModel.urgentSafetyMessage("Saya sakit dada dan sesak nafas"))
    }

    @Test
    fun urgentSafetyMessage_detectsSelfHarmPhrase() {
        assertNotNull(AiAssistantViewModel.urgentSafetyMessage("I am thinking about self harm"))
    }

    @Test
    fun urgentSafetyMessage_allowsRoutineMedicineQuestion() {
        assertNull(AiAssistantViewModel.urgentSafetyMessage("Paracetamol untuk apa?"))
    }
}
