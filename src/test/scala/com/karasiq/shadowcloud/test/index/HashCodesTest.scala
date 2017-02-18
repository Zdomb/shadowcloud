package com.karasiq.shadowcloud.test.index

import akka.util.ByteString
import com.karasiq.shadowcloud.test.utils.TestUtils
import org.scalatest.{FlatSpec, Matchers}

import scala.language.postfixOps

class HashCodesTest extends FlatSpec with Matchers {
  "Chunks" should "be compared" in {
    val chunk1 = TestUtils.randomChunk
    val chunk2 = chunk1.withoutData
    val chunk3 = chunk2.copy(checksum = chunk2.checksum.copy(encryptedSize = 0, encryptedHash = ByteString.empty))
    val chunk4 = chunk2.copy(checksum = chunk2.checksum.copy(encryptedSize = 0, encryptedHash = TestUtils.randomBytes(20)))
    val chunk5 = chunk2.copy(checksum = chunk2.checksum.copy(hash = TestUtils.randomBytes(20)))
    val chunk6 = chunk2.copy(checksum = chunk2.checksum.copy(size = chunk2.checksum.size + 1))
    chunk1 shouldBe chunk2
    chunk1 shouldBe chunk3
    chunk1 shouldNot be (chunk4)
    chunk1 shouldNot be (chunk5)
    chunk1 shouldNot be (chunk6)
    chunk1.hashCode() shouldBe chunk2.hashCode()
    chunk1.hashCode() shouldBe chunk3.hashCode()
    chunk1.hashCode() shouldBe chunk4.hashCode()
    chunk1.hashCode() shouldNot be (chunk5.hashCode())
    chunk1.hashCode() shouldNot be (chunk6.hashCode())
  }

  "Files" should "be compared" in {
    val file1 = TestUtils.randomFile()
    val file2 = file1.copy(checksum = file1.checksum.copy(encryptedSize = 0, encryptedHash = ByteString.empty))
    val file3 = file1.copy(checksum = file1.checksum.copy(encryptedSize = 0, encryptedHash = TestUtils.randomBytes(20)))
    val file4 = file1.copy(checksum = file1.checksum.copy(hash = TestUtils.randomBytes(20)))
    val file5 = file1.copy(checksum = file1.checksum.copy(size = file1.checksum.size + 1))
    val file6 = file1.copy(chunks = file1.chunks :+ TestUtils.randomChunk)
    file1 shouldBe file2
    file1 shouldNot be (file3)
    file1 shouldNot be (file4)
    file1 shouldNot be (file5)
    file1.hashCode() shouldBe file2.hashCode()
    file1.hashCode() shouldBe file3.hashCode()
    file1.hashCode() shouldNot be (file5.hashCode())
    file1.hashCode() shouldNot be (file6.hashCode())
  }

  "Folders" should "be compared" in {
    val folder1 = TestUtils.randomFolder()
    val folder2 = folder1.copy(created = folder1.created + 1, lastModified = folder1.lastModified + 1)
    val folder3 = folder1.withPath(folder1.path / "test")
    val folder4 = folder1.copy(folders = folder1.folders + TestUtils.randomString)
    val folder5 = folder1.copy(files = folder1.files + TestUtils.randomFile(folder1.path))
    folder2 shouldBe folder1
    folder3 shouldNot be (folder1)
    folder4 shouldNot be (folder1)
    folder5 shouldNot be (folder1)
    folder2.hashCode() shouldBe folder1.hashCode()
    folder3.hashCode() shouldNot be (folder1.hashCode())
    folder4.hashCode() shouldNot be (folder1.hashCode())
    folder5.hashCode() shouldNot be (folder1.hashCode())
  }
}