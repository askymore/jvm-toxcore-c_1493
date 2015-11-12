package sbt.tox4j.logic.jni

import java.io.{File, PrintWriter}

import org.apache.commons.io.FilenameUtils
import sbt._

import scala.collection.mutable.ArrayBuffer

object CMakeGenerator {

  private def generateFile(targetFile: File, lines: Seq[String]): File = {
    val out = new PrintWriter(targetFile)
    try {
      out.println(lines.mkString(System.lineSeparator))
    } finally {
      out.close()
    }

    targetFile
  }

  def dependenciesFile(
    includes: Seq[File],
    packageDependencies: Seq[String],
    nativeSource: File,
    nativeTarget: File,
    managedNativeSource: File
  ): File = {
    val targetFile = nativeTarget / "Dependencies.cmake"
    val lines = new ArrayBuffer[String]

    for (dir <- includes) {
      lines += s"include_directories($dir)"
    }

    if (packageDependencies.nonEmpty) {
      lines += "find_package(PkgConfig REQUIRED)"
    }
    packageDependencies foreach { pkg =>
      val PKG = pkg.toUpperCase.replace('-', '_')
      lines += s"pkg_check_modules($PKG REQUIRED $pkg)"
      lines += s"include_directories($${${PKG}_INCLUDE_DIRS})"
      lines += s"link_directories($${${PKG}_LIBRARY_DIRS})"
      lines += s"link_libraries($${${PKG}_LIBRARIES})"
    }

    Seq(nativeSource, nativeTarget, managedNativeSource) foreach { dir =>
      lines += s"include_directories($dir)"
    }

    generateFile(targetFile, lines)
  }

  def toolchainFile(
    toolchainPath: File
  )(
    nativeTarget: File,
    nativeCC: String,
    nativeCXX: String
  ): File = {
    val targetFile = nativeTarget / "Toolchain.cmake"
    val lines = new ArrayBuffer[String]

    lines += "SET(CMAKE_SYSTEM_NAME Linux)"
    lines += s"SET(CMAKE_C_COMPILER $nativeCC)"
    lines += s"SET(CMAKE_CXX_COMPILER $nativeCXX)"
    lines += s"SET(CMAKE_FIND_ROOT_PATH ${toolchainPath / "sysroot"})"
    lines += "SET(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)"
    lines += "SET(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)"
    lines += "SET(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)"

    generateFile(targetFile, lines)
  }

  def commonFile(log: Logger)(
    nativeTarget: File,
    cppFlags: Seq[String],
    cFlags: Seq[String],
    cxxFlags: Seq[String],
    ldFlags: Seq[String],
    featureTestFlags: Seq[String],
    coverageEnabled: Boolean,
    coverageFlags: Seq[String]
  ): File = {
    val coverageflags =
      if (coverageEnabled) {
        log.info(s"Coverage enabled: adding $coverageFlags to CXXFLAGS and LDFLAGS")
        coverageFlags
      } else {
        Nil
      }

    val cppflags = cppFlags ++ featureTestFlags
    val cflags = (cppflags ++ cFlags).distinct.mkString("\"", " ", "\"")
    val cxxflags = (cppflags ++ cxxFlags ++ coverageflags).distinct.mkString("\"", " ", "\"")
    val ldflags = (ldFlags ++ coverageflags).distinct.mkString("\"", " ", "\"")

    val targetFile = nativeTarget / "Common.cmake"
    val lines = new ArrayBuffer[String]

    lines += s"set(CMAKE_C_FLAGS $cflags)"
    lines += s"set(CMAKE_CXX_FLAGS $cxxflags)"
    lines += s"set(CMAKE_SHARED_LINKER_FLAGS $ldflags)"

    generateFile(targetFile, lines)
  }

  def mainFile(
    binPath: File,
    libraryName: String,
    nativeTarget: File,
    jniSourceFiles: Seq[File]
  ): File = {
    val targetFile = nativeTarget / "Main.cmake"
    val lines = new ArrayBuffer[String]

    lines += s"set(CMAKE_LIBRARY_OUTPUT_DIRECTORY $binPath)"
    lines += s"add_library($libraryName SHARED\n\t${jniSourceFiles.mkString("\n\t")})"

    generateFile(targetFile, lines)
  }

  def testFile(
    gtestPath: File
  )(
    libraryName: String,
    nativeTarget: File,
    jniSourceFiles: Seq[File]
  ): File = {
    val targetFile = nativeTarget / "Test.cmake"
    val lines = new ArrayBuffer[String]

    lines += s"add_library(gtest STATIC $gtestPath/src/gtest-all.cc)"
    lines += s"include_directories($gtestPath $gtestPath/include)"

    lines += "link_libraries(gtest)"

    lines += s"add_executable(${libraryName}_test\n\t${jniSourceFiles.mkString("\n\t")})"
    lines += s"#add_test(${libraryName}_test ${libraryName}_test)"

    jniSourceFiles.foreach { source =>
      if (source.getName != "main.cpp" && source.getName != "mock_jni.cpp") {
        val testName = FilenameUtils.removeExtension(source.getName)
        lines += s"add_executable($testName main.cpp mock_jni.cpp $source)"
        lines += s"add_test($testName $testName)"
      }
    }

    generateFile(targetFile, lines)
  }

}
