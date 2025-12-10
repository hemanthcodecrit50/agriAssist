package com.krishisakhi.farmassistant.personalization

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for UserProfileFileManager
 * Tests all core functionality with various scenarios
 *
 * Run with: ./gradlew test
 */
@RunWith(AndroidJUnit4::class)
class UserProfileFileManagerTest {

    private lateinit var context: Context
    private lateinit var manager: UserProfileFileManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = UserProfileFileManager(context)

        // Clean up any existing profile file before each test
        runBlocking {
            manager.deleteProfile()
        }
    }

    @After
    fun tearDown() {
        // Clean up after each test
        runBlocking {
            manager.deleteProfile()
        }
    }

    @Test
    fun testCreateOrLoadProfileFile_CreatesNewFile() = runBlocking {
        // Act
        val result = manager.createOrLoadProfileFile()

        // Assert
        assertTrue("File should be created", result)
        assertTrue("File should exist", manager.profileExists())
    }

    @Test
    fun testCreateOrLoadProfileFile_LoadsExistingFile() = runBlocking {
        // Arrange
        manager.createOrLoadProfileFile()

        // Act
        val result = manager.createOrLoadProfileFile()

        // Assert
        assertTrue("Should load existing file", result)
    }

    @Test
    fun testWriteProfile_SavesContent() = runBlocking {
        // Arrange
        val testContent = "I am a farmer from Maharashtra"

        // Act
        val result = manager.writeProfile(testContent)

        // Assert
        assertTrue("Write should succeed", result)
        val readContent = manager.readProfile()
        assertEquals("Content should match", testContent, readContent)
    }

    @Test
    fun testWriteProfile_OverwritesExisting() = runBlocking {
        // Arrange
        manager.writeProfile("First content")

        // Act
        val newContent = "Second content"
        manager.writeProfile(newContent)

        // Assert
        val readContent = manager.readProfile()
        assertEquals("Should contain new content", newContent, readContent)
        assertFalse("Should not contain old content", readContent.contains("First"))
    }

    @Test
    fun testWriteProfile_EmptyString() = runBlocking {
        // Arrange
        manager.writeProfile("Some content")

        // Act
        manager.writeProfile("")

        // Assert
        val content = manager.readProfile()
        assertEquals("Should be empty", "", content)
        assertFalse("Should not have content", manager.hasContent())
    }

    @Test
    fun testReadProfile_NonExistentFile() = runBlocking {
        // Act
        val content = manager.readProfile()

        // Assert
        assertEquals("Should return empty string", "", content)
        assertNotNull("Should never return null", content)
    }

    @Test
    fun testReadProfile_UTF8Encoding() = runBlocking {
        // Arrange
        val specialChars = "Test émojis 🌾🚜 and spëcial çharacters"
        manager.writeProfile(specialChars)

        // Act
        val readContent = manager.readProfile()

        // Assert
        assertEquals("UTF-8 characters should be preserved", specialChars, readContent)
    }

    @Test
    fun testAppendProfile_AddsContent() = runBlocking {
        // Arrange
        val initial = "Initial content"
        val append = " Additional content"
        manager.writeProfile(initial)

        // Act
        val result = manager.appendProfile(append)

        // Assert
        assertTrue("Append should succeed", result)
        val content = manager.readProfile()
        assertEquals("Should contain both parts", initial + append, content)
    }

    @Test
    fun testAppendProfile_EmptyFile() = runBlocking {
        // Act
        val text = "New content"
        val result = manager.appendProfile(text)

        // Assert
        assertTrue("Append should succeed", result)
        val content = manager.readProfile()
        assertEquals("Should contain appended text", text, content)
    }

    @Test
    fun testGetChunksForEmbedding_ShortText() = runBlocking {
        // Arrange
        val shortText = "Short profile text"
        manager.writeProfile(shortText)

        // Act
        val chunks = manager.getChunksForEmbedding()

        // Assert
        assertEquals("Should have 1 chunk", 1, chunks.size)
        assertEquals("Chunk should match original", shortText, chunks[0])
    }

    @Test
    fun testGetChunksForEmbedding_LongText() = runBlocking {
        // Arrange - Create text longer than chunk size (500)
        val longText = "A".repeat(1200)
        manager.writeProfile(longText)

        // Act
        val chunks = manager.getChunksForEmbedding()

        // Assert
        assertTrue("Should have multiple chunks", chunks.size > 1)
        assertTrue("Each chunk should be <= 500 chars", chunks.all { it.length <= 500 })
    }

    @Test
    fun testGetChunksForEmbedding_EmptyFile() = runBlocking {
        // Act
        val chunks = manager.getChunksForEmbedding()

        // Assert
        assertTrue("Should return empty list", chunks.isEmpty())
    }

    @Test
    fun testGetChunksForEmbedding_WithOverlap() = runBlocking {
        // Arrange - Text with clear words
        val text = "word " * 200 // 1000 characters
        manager.writeProfile(text)

        // Act
        val chunks = manager.getChunksForEmbedding()

        // Assert
        assertTrue("Should have multiple chunks", chunks.size >= 2)
        // Check overlap: last words of chunk N should appear in chunk N+1
        if (chunks.size >= 2) {
            val chunk1End = chunks[0].takeLast(50)
            val chunk2Start = chunks[1].take(50)
            // Should have some overlap
            assertTrue("Chunks should overlap",
                chunks[1].contains(chunk1End.split(" ").last()))
        }
    }

    @Test
    fun testProfileExists_WhenCreated() = runBlocking {
        // Act
        manager.createOrLoadProfileFile()

        // Assert
        assertTrue("Profile should exist", manager.profileExists())
    }

    @Test
    fun testProfileExists_WhenNotCreated() = runBlocking {
        // Act & Assert
        assertFalse("Profile should not exist", manager.profileExists())
    }

    @Test
    fun testHasContent_WithContent() = runBlocking {
        // Arrange
        manager.writeProfile("Some content")

        // Act & Assert
        assertTrue("Should have content", manager.hasContent())
    }

    @Test
    fun testHasContent_EmptyFile() = runBlocking {
        // Arrange
        manager.writeProfile("")

        // Act & Assert
        assertFalse("Should not have content", manager.hasContent())
    }

    @Test
    fun testGetFileSize_WithContent() = runBlocking {
        // Arrange
        val content = "Test content"
        manager.writeProfile(content)

        // Act
        val size = manager.getFileSize()

        // Assert
        assertTrue("Size should be > 0", size > 0)
        assertEquals("Size should match content",
            content.toByteArray(Charsets.UTF_8).size.toLong(), size)
    }

    @Test
    fun testGetFileSize_EmptyFile() = runBlocking {
        // Arrange
        manager.writeProfile("")

        // Act
        val size = manager.getFileSize()

        // Assert
        assertEquals("Size should be 0", 0L, size)
    }

    @Test
    fun testDeleteProfile_RemovesFile() = runBlocking {
        // Arrange
        manager.writeProfile("Content to delete")

        // Act
        val result = manager.deleteProfile()

        // Assert
        assertTrue("Delete should succeed", result)
        assertFalse("File should not exist", manager.profileExists())
    }

    @Test
    fun testDeleteProfile_NonExistentFile() = runBlocking {
        // Act
        val result = manager.deleteProfile()

        // Assert
        assertTrue("Should return true for non-existent file", result)
    }

    @Test
    fun testClearProfile_RemovesContent() = runBlocking {
        // Arrange
        manager.writeProfile("Content to clear")

        // Act
        val result = manager.clearProfile()

        // Assert
        assertTrue("Clear should succeed", result)
        assertTrue("File should still exist", manager.profileExists())
        assertFalse("Should not have content", manager.hasContent())
    }

    @Test
    fun testGetProfileStats_CompleteInfo() = runBlocking {
        // Arrange
        val content = "Test content with multiple words.\nAnd multiple lines."
        manager.writeProfile(content)

        // Act
        val stats = manager.getProfileStats()

        // Assert
        assertTrue("Should exist", stats.exists)
        assertTrue("Should have size", stats.fileSizeBytes > 0)
        assertEquals("Character count", content.length, stats.characterCount)
        assertTrue("Should have words", stats.wordCount > 0)
        assertEquals("Line count", 2, stats.lineCount)
        assertEquals("Chunk count", 1, stats.chunkCount)
        assertTrue("Should have path", stats.filePath.isNotEmpty())
    }

    @Test
    fun testGetProfileStats_EmptyFile() = runBlocking {
        // Act
        val stats = manager.getProfileStats()

        // Assert
        assertFalse("Should not exist", stats.exists)
        assertEquals("Should have 0 size", 0L, stats.fileSizeBytes)
        assertEquals("Should have 0 characters", 0, stats.characterCount)
    }

    @Test
    fun testValidateProfile_ValidContent() = runBlocking {
        // Arrange
        val validContent = "This is a valid farmer profile with sufficient content."
        manager.writeProfile(validContent)

        // Act
        val validation = manager.validateProfile()

        // Assert
        assertTrue("Should be valid", validation.isValid)
        assertFalse("Should not have errors", validation.hasErrors())
    }

    @Test
    fun testValidateProfile_EmptyContent() = runBlocking {
        // Arrange
        manager.writeProfile("")

        // Act
        val validation = manager.validateProfile()

        // Assert
        assertTrue("Should be valid", validation.isValid)
        assertTrue("Should have warning", validation.hasWarnings())
        assertTrue("Should mention empty",
            validation.warnings.any { it.contains("empty") })
    }

    @Test
    fun testValidateProfile_ShortContent() = runBlocking {
        // Arrange
        manager.writeProfile("Short")

        // Act
        val validation = manager.validateProfile()

        // Assert
        assertTrue("Should be valid", validation.isValid)
        assertTrue("Should have warning", validation.hasWarnings())
        assertTrue("Should mention short",
            validation.warnings.any { it.contains("short") })
    }

    @Test
    fun testValidateProfile_VeryLongContent() = runBlocking {
        // Arrange
        val longContent = "A".repeat(6000)
        manager.writeProfile(longContent)

        // Act
        val validation = manager.validateProfile()

        // Assert
        assertTrue("Should be valid", validation.isValid)
        assertTrue("Should have warning", validation.hasWarnings())
        assertTrue("Should mention long",
            validation.warnings.any { it.contains("long") })
    }

    @Test
    fun testMultipleOperations_Sequence() = runBlocking {
        // Test complete workflow

        // 1. Create
        assertTrue(manager.createOrLoadProfileFile())

        // 2. Write
        assertTrue(manager.writeProfile("Initial"))
        assertEquals("Initial", manager.readProfile())

        // 3. Append
        assertTrue(manager.appendProfile(" Added"))
        assertEquals("Initial Added", manager.readProfile())

        // 4. Check stats
        val stats = manager.getProfileStats()
        assertTrue(stats.exists)
        assertTrue(stats.characterCount > 0)

        // 5. Get chunks
        val chunks = manager.getChunksForEmbedding()
        assertEquals(1, chunks.size)

        // 6. Validate
        val validation = manager.validateProfile()
        assertTrue(validation.isValid)

        // 7. Clear
        assertTrue(manager.clearProfile())
        assertFalse(manager.hasContent())

        // 8. Delete
        assertTrue(manager.deleteProfile())
        assertFalse(manager.profileExists())
    }

    @Test
    fun testConcurrency_MultipleWrites() = runBlocking {
        // Test rapid consecutive writes
        repeat(10) { i ->
            val result = manager.writeProfile("Content $i")
            assertTrue("Write $i should succeed", result)
        }

        // Last write should win
        val content = manager.readProfile()
        assertEquals("Content 9", content)
    }

    @Test
    fun testGetFilePath_ReturnsValidPath() {
        // Act
        val path = manager.getFilePath()

        // Assert
        assertNotNull("Path should not be null", path)
        assertTrue("Path should contain filename", path.contains("farmer_profile.txt"))
        assertTrue("Path should be absolute", path.startsWith("/"))
    }
}

