package com.example

import com.example.engine.AgentTools
import com.example.engine.DiffGenerator
import com.example.engine.DiffType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun diffGenerator_detectsAdditionsAndDeletions() {
        val oldCode = "line 1\nline 2\nline 3"
        val newCode = "line 1\nline 2 modified\nline 3\nline 4"

        val diff = DiffGenerator.computeDiff(oldCode, newCode)

        assertTrue(diff.isNotEmpty())
        assertTrue(diff.any { it.type == DiffType.ADD && it.text == "line 4" })
        assertTrue(diff.any { it.type == DiffType.DELETE && it.text == "line 2" })
        assertTrue(diff.any { it.type == DiffType.ADD && it.text == "line 2 modified" })

        val patch = DiffGenerator.formatPatch(diff)
        assertTrue(patch.contains("+ line 4"))
        assertTrue(patch.contains("- line 2"))
    }

    @Test
    fun agentTools_containsCoreHermesCapabilities() {
        val toolNames = AgentTools.DEFINITIONS.map { it.function.name }

        assertTrue(toolNames.contains("read_file"))
        assertTrue(toolNames.contains("write_file"))
        assertTrue(toolNames.contains("list_files"))
        assertTrue(toolNames.contains("delete_file"))
        assertTrue(toolNames.contains("save_memory"))
        assertTrue(toolNames.contains("create_skill"))
    }
}
