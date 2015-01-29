/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2012-2014 the original author or authors.
 */
package org.assertj.core.api;

import org.assertj.core.internal.Paths;
import org.assertj.core.util.PathsException;
import org.assertj.core.util.VisibleForTesting;

import java.io.IOException;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;

/**
 * Assertions for {@link Path} objects
 *
 * <p>
 * Note that some assertions have two versions: a normal one and a "raw" one (for instance, {@code hasParent()} and
 * {@code hasParentRaw()}. The difference is that normal assertions will {@link Path#toRealPath(LinkOption...)
 * canonicalize} or {@link Path#normalize() normalize} the tested path and, where applicable, the path argument, before
 * performing the actual test. Canonicalization includes normalization.
 * </p>
 *
 * <p>
 * Canonicalization may lead to an I/O error if a path does not exist, in which case the given assertions will fail with
 * a {@link PathsException}. Also note that {@link Files#isSymbolicLink(Path) symbolic links} will be followed if the
 * filesystem supports them. Finally, if a path is not {@link Path#isAbsolute()} absolute}, canonicalization will
 * resolve the path against the process' current working directory.
 * </p>
 *
 * <p>
 * These assertions are filesystem independent. You may use them on {@code Path} instances issued from the default
 * filesystem (ie, instances you get when using {@link java.nio.file.Paths#get(String, String...)}) or from other
 * filesystems. For more information, see the {@link FileSystem javadoc for {@code FileSystem} .
 * </p>
 *
 * <p>
 * Furthermore:
 * </p>
 *
 * <ul>
 * <li>Unless otherwise noted, assertions which accept arguments will not accept {@code null} arguments; if a null
 * argument is passed, these assertions will throw a {@link NullPointerException}.</li>
 * <li>It is the caller's responsibility to ensure that paths used in assertions are issued from valid filesystems which
 * are not {@link FileSystem#close() closed}. If a filesystems is closed, assertions will throw a
 * {@link ClosedFileSystemException}.</li>
 * <li>Some assertions take another {@link Path} as an argument. If this path is not issued from the same
 * {@link FileSystemProvider provider} as the tested path, assertions will throw a {@link ProviderMismatchException}.</li>
 * <li>Some assertions may need to perform I/O on the path's underlying filesystem; if an I/O error occurs when
 * accessing the filesystem, these assertions will throw a {@link PathsException}.</li>
 * </ul>
 *
 * @param <S> self type
 *
 * @see Path
 * @see java.nio.file.Paths#get(String, String...)
 * @see FileSystem
 * @see FileSystem#getPath(String, String...)
 * @see FileSystems#getDefault()
 * @see Files
 */
public abstract class AbstractPathAssert<S extends AbstractPathAssert<S>> extends AbstractAssert<S, Path> {

  @VisibleForTesting
  protected Paths paths = Paths.instance();

  protected AbstractPathAssert(final Path actual, final Class<?> selfType) {
	super(actual, selfType);
  }

  // TODO isWritable
  // TODO isExecutable
  // TODO hasFileName
  // TODO containsName ?

  /**
   * Assert that the tested {@link Path} is a readable file, it checks that the file exists and that this Java virtual
   * machine has appropriate privileges that would allow it open the file for reading.
   *
   * <p>
   * Examples:
   * </p>
   *
   * <pre><code class="java">
   * // Create a file and set permissions to be readable by all.
   * Path readableFile = Paths.get("readableFile");
   * Set&lt;PosixFilePermission&gt; perms = PosixFilePermissions.fromString("r--r--r--");
   * Files.createFile(readableFile, PosixFilePermissions.asFileAttribute(perms));
   * 
   * final Path symlinkToReadableFile = fs.getPath("symlinkToReadableFile");
   * Files.createSymbolicLink(symlinkToReadableFile, readableFile);
   * 
   * // Create a file and set permissions not to be readable.
   * Path nonReadableFile = Paths.get("nonReadableFile");
   * Set&lt;PosixFilePermission&gt; perms = PosixFilePermissions.fromString("-wx------");
   * Files.createFile(nonReadableFile, PosixFilePermissions.asFileAttribute(perms));
   * 
   * final Path nonExistentPath = fs.getPath("nonexistent");
   *
   * // The following assertions succeed:
   * assertThat(readableFile).isReadable();
   * assertThat(symlinkToReadableFile).isReadable();
   *
   * // The following assertions fail:
   * assertThat(nonReadableFile).isReadable();
   * assertThat(nonExistentPath).isReadable();
   * </code></pre>
   *
   * @return self
   * @throws IOException
   *
   * @see Files#isReadable(Path)
   */
  public S isReadable() {
	paths.assertIsReadable(info, actual);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} exists.
   *
   * <p>
   * <strong>Note that this assertion will follow symbolic links before asserting the path's existence.</strong>
   * </p>
   *
   * <p>
   * On Windows system, this has no influence. On Unix systems, this means the assertion result will fail if the path is
   * a symbolic link whose target does not exist. If you want to assert the existence of the symbolic link itself, use
   * {@link #existsNoFollowLinks()} instead.
   * </p>
   *
   * <p>
   * Examples:
   * </p>
   *
   * <pre><code class="java">
   * // fs is a Unix filesystem
   * // Create a regular file, and a symbolic link pointing to it
   * final Path existingFile = fs.getPath("somefile");
   * Files.createFile(existingFile);
   * final Path symlinkToExistingFile = fs.getPath("symlinkToExistingFile");
   * Files.createSymbolicLink(symlinkToExistingFile, existingFile);
   *
   * // Create a symbolic link whose target does not exist
   * final Path nonExistentPath = fs.getPath("nonexistent");
   * final Path symlinkToNonExistentPath = fs.getPath("symlinkToNonExistentPath");
   * Files.createSymbolicLink(symlinkToNonExistentPath, nonExistentPath);
   *
   * // The following assertions succeed:
   * assertThat(existingFile).exists();
   * assertThat(symlinkToExistingFile).exists();
   *
   * // The following assertions fail:
   * assertThat(nonExistentPath).exists();
   * assertThat(symlinkToNonExistentPath).exists();
   * </code></pre>
   *
   * @return self
   *
   * @see Files#exists(Path, LinkOption...)
   */
  public S exists() {
	paths.assertExists(info, actual);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} exists, not following symbolic links.
   *
   * <p>
   * This assertion behaves like {@link #exists()}, with the difference that it can be used to assert the existence of a
   * symbolic link even if its target is invalid.
   * </p>
   *
   * <p>
   * Examples:
   * </p>
   *
   * <pre><code class="java">
   * // fs is a Unix filesystem
   * // Create a regular file, and a symbolic link pointing to it
   * final Path existingFile = fs.getPath("somefile");
   * Files.createFile(existingFile);
   * final Path symlinkToExistingFile = fs.getPath("symlink");
   * Files.createSymbolicLink(symlinkToExistingFile, existingFile);
   * 
   * // Create a symbolic link whose target does not exist
   * final Path nonExistentPath = fs.getPath("nonexistent");
   * final Path symlinkToNonExistentPath = fs.getPath("symlinkToNonExistentPath");
   * Files.createSymbolicLink(symlinkToNonExistentPath, nonExistentPath);
   *
   * // The following assertions succeed
   * assertThat(existingFile).existsNoFollowLinks();
   * assertThat(symlinkToExistingFile).existsNoFollowLinks();
   * assertThat(symlinkToNonExistentPath).existsNoFollowLinks();
   *
   * // The following assertion fails
   * assertThat(nonExistentPath).existsNoFollowLinks();
   * </code></pre>
   *
   * @return self
   *
   * @see Files#exists(Path, LinkOption...)
   */
  public S existsNoFollowLinks() {
	paths.assertExistsNoFollowLinks(info, actual);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} does not exist.
   *
   * <p>
   * <strong>IMPORTANT NOTE:</strong> this method will NOT follow symbolic links (provided that the underlying
   * {@link FileSystem} of this path supports symbolic links at all).
   * </p>
   *
   * <p>
   * This means that even if the path to test is a symbolic link whose target does not exist, this assertion will
   * consider that the path exists (note that this is unlike the default behavior of {@link #exists()}).
   * </p>
   *
   * <p>
   * If you are a Windows user, the above does not apply to you; if you are a Unix user however, this is important.
   * Consider the following:
   * </p>
   *
   * <pre><code class="java">
   * // fs is a FileSystem
   * // Create a regular file, and a symbolic link pointing to it
   * final Path existingFile = fs.getPath("somefile");
   * Files.createFile(existingFile);
   * final Path symlinkToExistingFile = fs.getPath("symlink");
   * Files.createSymbolicLink(symlinkToExistingFile, existingFile);
   * 
   * // Create a symbolic link to a nonexistent target file.
   * final Path nonExistentPath = fs.getPath("nonExistentPath");
   * final Path symlinkToNonExistentPath = fs.getPath("symlinkToNonExistentPath");
   * Files.createSymbolicLink(symlinkToNonExistentPath, nonExistentPath);
   *
   * // The following assertion succeeds
   * assertThat(nonExistentPath).doesNotExist();
   * 
   * // The following assertions fail:
   * assertThat(existingFile).doesNotExist();
   * assertThat(symlinkToExistingFile).doesNotExist();
   * assertThat(symlinkToNonExistentPath).doesNotExist();
   * </code></pre>
   *
   * @return self
   *
   * @see Files#notExists(Path, LinkOption...)
   * @see LinkOption#NOFOLLOW_LINKS
   */
  public S doesNotExist() {
	paths.assertDoesNotExist(info, actual);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} is a regular file.
   *
   * <p>
   * <strong>Note that this method will follow symbolic links.</strong> If you are a Unix user and wish to assert that a
   * path is a symbolic link instead, use {@link #isSymbolicLink()}.
   * </p>
   *
   * <p>
   * This assertion first asserts the existence of the path (using {@link #exists()}) then checks whether the path is a
   * regular file.
   * </p>
   *
   * <p>
   * Examples:
   * </p>
   *
   * <pre><code class="java">
   * // fs is a Unix filesystem
   *
   * // Create a regular file, and a symbolic link to that regular file
   * final Path existingFile = fs.getPath("existingFile");
   * final Path symlinkToExistingFile = fs.getPath("symlinkToExistingFile");
   * Files.createFile(existingFile);
   * Files.createSymbolicLink(symlinkToExistingFile, existingFile);
   *
   * // Create a directory, and a symbolic link to that directory
   * final Path dir = fs.getPath("dir");
   * final Path dirSymlink = fs.getPath("dirSymlink");
   * Files.createDirectories(dir);
   * Files.createSymbolicLink(dirSymlink, dir);
   *
   * // Create a nonexistent entry, and a symbolic link to that entry
   * final Path nonExistentPath = fs.getPath("nonexistent");
   * final Path symlinkToNonExistentPath = fs.getPath("symlinkToNonExistentPath");
   * Files.createSymbolicLink(symlinkToNonExistentPath, nonExistentPath);
   *
   * // the following assertions succeed:
   * assertThat(existingFile).isRegularFile();
   * assertThat(symlinkToExistingFile).isRegularFile();
   *
   * // the following assertions fail because paths do not exist:
   * assertThat(nonExistentPath).isRegularFile();
   * assertThat(symlinkToNonExistentPath).isRegularFile();
   *
   * // the following assertions fail because paths exist but are not regular files:
   * assertThat(dir).isRegularFile();
   * assertThat(dirSymlink).isRegularFile();
   * </code></pre>
   *
   * @return self
   */
  public S isRegularFile() {
	paths.assertIsRegularFile(info, actual);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} is a directory.
   * <p>
   * <strong>Note that this method will follow symbolic links.</strong> If you are a Unix user and wish to assert that a
   * path is a symbolic link instead, use {@link #isSymbolicLink()}.
   * </p>
   *
   * <p>
   * This assertion first asserts the existence of the path (using {@link #exists()}) then checks whether the path is a
   * directory.
   * </p>
   *
   * <p>
   * Examples:
   * </p>
   *
   * <pre><code class="java">
   * // fs is a Unix filesystem
   *
   * // Create a regular file, and a symbolic link to that regular file
   * final Path existingFile = fs.getPath("existingFile");
   * final Path symlinkToExistingFile = fs.getPath("symlinkToExistingFile");
   * Files.createFile(existingFile);
   * Files.createSymbolicLink(symlinkToExistingFile, existingFile);
   *
   * // Create a directory, and a symbolic link to that directory
   * final Path dir = fs.getPath("dir");
   * final Path dirSymlink = fs.getPath("dirSymlink");
   * Files.createDirectories(dir);
   * Files.createSymbolicLink(dirSymlink, dir);
   *
   * // Create a nonexistent entry, and a symbolic link to that entry
   * final Path nonExistentPath = fs.getPath("nonexistent");
   * final Path symlinkToNonExistentPath = fs.getPath("symlinkToNonExistentPath");
   * Files.createSymbolicLink(symlinkToNonExistentPath, nonExistentPath);
   *
   * // the following assertions succeed:
   * assertThat(dir).isDirectory();
   * assertThat(dirSymlink).isDirectory();
   *
   * // the following assertions fail because paths do not exist:
   * assertThat(nonExistentPath).isDirectory();
   * assertThat(symlinkToNonExistentPath).isDirectory();
   *
   * // the following assertions fail because paths exist but are not directories:
   * assertThat(existingFile).isDirectory();
   * assertThat(symlinkToExistingFile).isDirectory();
   * </code></pre>
   *
   * @return self
   */
  public S isDirectory() {
	paths.assertIsDirectory(info, actual);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} is a symbolic link.
   * <p>
   * This assertion first asserts the existence of the path (using {@link #existsNoFollowLinks()}) then checks whether
   * the path is a symbolic link.
   * </p>
   *
   * <p>
   * Examples:
   * </p>
   *
   * <pre><code class="java">
   * // fs is a Unix filesystem
   *
   * // Create a regular file, and a symbolic link to that regular file
   * final Path existingFile = fs.getPath("existingFile");
   * final Path symlinkToExistingFile = fs.getPath("symlinkToExistingFile");
   * Files.createFile(existingFile);
   * Files.createSymbolicLink(symlinkToExistingFile, existingFile);
   *
   * // Create a directory, and a symbolic link to that directory
   * final Path dir = fs.getPath("dir");
   * final Path dirSymlink = fs.getPath("dirSymlink");
   * Files.createDirectories(dir);
   * Files.createSymbolicLink(dirSymlink, dir);
   *
   * // Create a nonexistent entry, and a symbolic link to that entry
   * final Path nonExistentPath = fs.getPath("nonexistent");
   * final Path symlinkToNonExistentPath = fs.getPath("symlinkToNonExistentPath");
   * Files.createSymbolicLink(symlinkToNonExistentPath, nonExistentPath);
   *
   * // the following assertions succeed:
   * assertThat(dirSymlink).isSymbolicLink();
   * assertThat(symlinkToExistingFile).isSymbolicLink();
   * assertThat(symlinkToNonExistentPath).isSymbolicLink();
   *
   * // the following assertion fails because the path does not exist:
   * assertThat(nonExistentPath).isSymbolicLink();
   *
   * // the following assertions fail because paths exist but are not symbolic links
   * assertThat(existingFile).isSymbolicLink();
   * assertThat(dir).isSymbolicLink();
   * </code></pre>
   *
   * @return self
   */
  public S isSymbolicLink() {
	paths.assertIsSymbolicLink(info, actual);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} is absolute.
   *
   * <p>
   * Note that the fact that a path is absolute does not mean that it is {@link Path#normalize() normalized}:
   * {@code /foo/..} is absolute, for instance, but it is not normalized.
   * </p>
   *
   * <p>
   * Examples:
   * </p>
   *
   * <pre><code class="java">
   * // unixFs is a Unix FileSystem
   *
   * // The following assertion succeeds:
   * assertThat(unixFs.getPath("/foo/bar")).isAbsolute();
   *
   * // The following assertion fails:
   * assertThat(unixFs.getPath("foo/bar")).isAbsolute();
   *
   * // windowsFs is a Windows FileSystem
   *
   * // The following assertion succeeds:
   * assertThat(windowsFs.getPath("c:\\foo")).isAbsolute();
   *
   * // The following assertions fail:
   * assertThat(windowsFs.getPath("foo\\bar")).isAbsolute();
   * assertThat(windowsFs.getPath("c:foo")).isAbsolute();
   * assertThat(windowsFs.getPath("\\foo\\bar")).isAbsolute();
   * </code></pre>
   *
   * @return self
   *
   * @see Path#isAbsolute()
   */
  public S isAbsolute() {
	paths.assertIsAbsolute(info, actual);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} is relative (opposite to {@link Path#isAbsolute()}).
   *
   * <p>
   * Examples:
   * </p>
   * 
   * <pre><code class="java">
   * // unixFs is a Unix FileSystem
   *
   * // The following assertions succeed:
   * assertThat(unixFs.getPath("./foo/bar")).isRelative();
   * assertThat(unixFs.getPath("foo/bar")).isRelative();
   *
   * // The following assertion fails:
   * assertThat(unixFs.getPath("/foo/bar")).isRelative();
   *
   * // windowsFs is a Windows FileSystem
   *
   * // The following assertion succeeds:
   * assertThat(windowsFs.getPath("foo\\bar")).isRelative();
   * assertThat(windowsFs.getPath("c:foo")).isRelative();
   * assertThat(windowsFs.getPath("\\foo\\bar")).isRelative();
   *
   * // The following assertions fail:
   * assertThat(windowsFs.getPath("c:\\foo")).isRelative();
   * </code></pre>
   *
   * @return self
   *
   * @see Path#isAbsolute()
   */
  public S isRelative() {
	paths.assertIsRelative(info, actual);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} is normalized.
   *
   * <p>
   * A path is normalized if it has no redundant components; typically, on both Unix and Windows, this means that the
   * path has no "self" components ({@code .}) and that its only parent components ({@code ..}), if any, are at the
   * beginning of the path.
   * </p>
   *
   * <p>
   * Examples:
   * </p>
   *
   * <pre><code class="java">
   * // fs is a Unix filesystem
   *
   * // the following assertions succeed:
   * assertThat(fs.getPath("/usr/lib")).isNormalized();
   * assertThat(fs.getPath("a/b/c")).isNormalized();
   * assertThat(fs.getPath("../d")).isNormalized();
   *
   * // the following assertions fail:
   * assertThat(fs.getPath("/a/./b")).isNormalized();
   * assertThat(fs.getPath("c/b/..")).isNormalized();
   * assertThat(fs.getPath("/../../e")).isNormalized();
   * </code></pre>
   *
   * @return self
   */
  public S isNormalized() {
	paths.assertIsNormalized(info, actual);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} is canonical by comparing it to its {@link Path#toRealPath(LinkOption...) real
   * path}.
   *
   * <p>
   * For Windows users, this assertion is no different than {@link #isAbsolute()}. For Unix users, this assertion
   * ensures that the tested path is the actual file system resource, that is, it is not a
   * {@link Files#isSymbolicLink(Path) symbolic link} to the actual resource, even if the path is absolute.
   * </p>
   *
   * <p>
   * Examples:
   * </p>
   *
   * <pre><code class="java">
   * // fs is a Unix filesystem
   * // Create a directory
   * final Path basedir = fs.getPath("/tmp/foo");
   * Files.createDirectories(basedir);
   * 
   * // Create a file in this directory
   * final Path existingFile = basedir.resolve("existingFile");
   * Files.createFile(existingFile);
   * 
   * // Create a symbolic link to that file
   * final Path symlinkToExistingFile = basedir.resolve("symlinkToExistingFile");
   * Files.createSymbolicLink(symlinkToExistingFile, existingFile);
   *
   * // The following assertion succeeds:
   * assertThat(existingFile).isCanonical();
   *
   * // The following assertion fails:
   * assertThat(symlinkToExistingFile).isCanonical();
   * </code></pre>
   *
   * @throws PathsException an I/O error occurred while evaluating the path
   *
   * @see Path#toRealPath(LinkOption...)
   * @see Files#isSameFile(Path, Path)
   */
  public S isCanonical() {
	paths.assertIsCanonical(info, actual);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} has the expected parent path.
   *
   * <p>
   * <em>This assertion will perform canonicalization of the tested path and of the given argument before performing the test; see the class
   * description for more details. If this is not what you want, use {@link #hasParentRaw(Path)} instead.</em>
   * </p>
   *
   * <p>
   * Checks that the tested path has the given parent. This assertion will fail both if the tested path has no parent,
   * or has a different parent than what is expected.
   * </p>
   *
   * <p>
   * Examples:
   * </p>
   *
   * <pre><code class="java">
   * // fs is a Unix filesystem
   * final Path actual = fs.getPath("/dir1/dir2/file");
   *
   * // the following assertion succeeds:
   * assertThat(actual).hasParent(fs.getPath("/dir1/dir2/."));
   * // this one too as this path will be normalized to "/dir1/dir2":
   * assertThat(actual).hasParent(fs.getPath("/dir1/dir3/../dir2/."));
   *
   * // the following assertion fails:
   * assertThat(actual).hasParent(fs.getPath("/dir1"));
   * </code></pre>
   *
   * @param expected the expected parent path
   * @return self
   *
   * @see Path#getParent()
   */
  public S hasParent(final Path expected) {
	paths.assertHasParent(info, actual, expected);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} has the expected parent path.
   *
   * <p>
   * <em>This assertion will not perform any canonicalization of either the tested path or the path given as an argument; 
   * see class description for more details. If this is not what you want, use {@link #hasParent(Path)} instead.</em>
   * </p>
   *
   * <p>
   * This assertion uses {@link Path#getParent()} with no modification, which means the only criterion for this
   * assertion's success is the path's components (its root and its name elements).
   * </p>
   *
   * <p>
   * This may lead to surprising results if the tested path and the path given as an argument are not normalized. For
   * instance, if the tested path is {@code /home/foo/../bar} and the argument is {@code /home}, the assertion will
   * <em>fail</em> since the parent of the tested path is not {@code /home} but... {@code /home/foo/..}.
   * </p>
   *
   * <p>
   * While this may seem counterintuitive, it has to be recalled here that it is not required for a {@link FileSystem}
   * to consider that {@code .} and {@code ..} are name elements for respectively the current directory and the parent
   * directory respectively. In fact, it is not even required that a {@link FileSystem} be hierarchical at all.
   * </p>
   *
   * <p>
   * Examples:
   * </p>
   *
   * <pre><code class="java">
   * // fs is a Unix filesystem
   * final Path actual = fs.getPath("/dir1/dir2/file");
   *
   * // the following assertion succeeds:
   * assertThat(actual).hasParentRaw(fs.getPath("/dir1/dir2"));
   *
   * // the following assertions fails:
   * assertThat(actual).hasParent(fs.getPath("/dir1"));
   * // ... and this one too as expected path is not canonicalized.
   * assertThat(actual).hasParentRaw(fs.getPath("/dir1/dir3/../dir2"));
   * </code></pre>
   *
   * @param expected the expected parent path
   * @return self
   *
   * @see Path#getParent()
   */
  public S hasParentRaw(final Path expected) {
	paths.assertHasParentRaw(info, actual, expected);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} has no parent.
   *
   * <p>
   * <em>This assertion will first canonicalize the tested path before performing the test; if this is not what you want, use {@link #hasNoParentRaw()} instead.</em>
   * </p>
   *
   * <p>
   * Check that the tested path, after canonicalization, has no parent. See the class description for more information
   * about canonicalization.
   * </p>
   *
   * <p>
   * Examples:
   * </p>
   *
   * <pre><code class="java">
   * // unixFs is a Unix filesystem
   *
   * // the following assertion succeeds:
   * assertThat(fs.getPath("/")).hasNoParent();
   * // this one too as path will be normalized to "/"
   * assertThat(fs.getPath("/usr/..")).hasNoParent();
   *
   * // the following assertions fail:
   * assertThat(fs.getPath("/usr/lib")).hasNoParent();
   * assertThat(fs.getPath("/usr")).hasNoParent();
   * </code></pre>
   *
   * @return self
   *
   * @throws PathsException failed to canonicalize the tested path
   *
   * @see Path#getParent()
   */
  public S hasNoParent() {
	paths.assertHasNoParent(info, actual);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} has no parent.
   *
   * <p>
   * <em>This assertion will not canonicalize the tested path before performing the test; 
   * if this is not what you want, use {@link #hasNoParent()} instead.</em>
   * </p>
   *
   * <p>
   * As canonicalization is not performed, this means the only criterion for this assertion's success is the path's
   * components (its root and its name elements).
   * </p>
   *
   * <p>
   * This may lead to surprising results. For instance, path {@code /usr/..} <em>does</em> have a parent, and this
   * parent is {@code /usr}.
   * </p>
   *
   * <p>
   * Examples:
   * </p>
   *
   * <pre><code class="java">
   * // unixFs is a Unix filesystem
   *
   * // the following assertions succeed:
   * assertThat(fs.getPath("/")).hasNoParentRaw();
   * assertThat(fs.getPath("foo")).hasNoParentRaw();
   *
   * // the following assertions :
   * assertThat(fs.getPath("/usr/lib")).hasNoParentRaw();
   * assertThat(fs.getPath("/usr")).hasNoParentRaw();
   * // this one fails as canonicalization is not performed, leading to parent being /usr
   * assertThat(fs.getPath("/usr/..")).hasNoParent();
   * </code></pre>
   *
   * @return self
   *
   * @see Path#getParent()
   */
  public S hasNoParentRaw() {
	paths.assertHasNoParentRaw(info, actual);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} starts with the given path.
   *
   * <p>
   * <em>This assertion will perform canonicalization of both the tested path and the path given as an argument; 
   * see class description for more details. If this is not what you want, use {@link #startsWithRaw(Path)} instead.</em>
   * </p>
   *
   * <p>
   * Checks that the given {@link Path} starts with another path. Note that the name components matter, not the string
   * representation; this means that, for example, {@code /home/foobar/baz} <em>does not</em> start with
   * {@code /home/foo}.
   * </p>
   *
   * <p>
   * Examples:
   * </p>
   *
   * <pre><code class="java">
   * // fs is a Unix filesystem
   * final Path tested = fs.getPath("/home/joe/myfile");
   *
   * // the following assertion succeeds:
   * assertThat(tested).startsWith(fs.getPath("/home"));
   * assertThat(tested).startsWith(fs.getPath("/home/"));
   * assertThat(tested).startsWith(fs.getPath("/home/."));
   * // assertion succeeds because this path will be canonicalized to "/home/joe"
   * assertThat(tested).startsWith(fs.getPath("/home/jane/../joe/."));
   *
   * // the following assertion fails:
   * assertThat(tested).startsWith(fs.getPath("/home/harry"));
   * </code></pre>
   *
   * @param other the other path
   * @return self
   *
   * @throws PathsException failed to canonicalize the tested path or the path given as an argument
   *
   * @see Path#startsWith(Path)
   * @see Path#toRealPath(LinkOption...)
   */
  public S startsWith(final Path other) {
	paths.assertStartsWith(info, actual, other);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} starts with the given path.
   *
   * <p>
   * <em>This assertions does not perform canonicalization on either the
   * tested path or the path given as an argument; see class description for
   * more details. If this is not what you want, use {@link #startsWith(Path)}
   * instead.</em>
   * </p>
   *
   * <p>
   * Checks that the given {@link Path} starts with another path, without performing canonicalization on its arguments.
   * This means that the only criterion to determine whether a path starts with another is the tested path's, and the
   * argument's, name elements.
   * </p>
   *
   * <p>
   * This may lead to some surprising results: for instance, path {@code /../home/foo} does <em>not</em> start with
   * {@code /home} since the first name element of the former ({@code ..}) is different from the first name element of
   * the latter ({@code home}).
   * </p>
   *
   * <p>
   * Examples:
   * </p>
   *
   * <pre><code class="java">
   * // fs is a Unix filesystem
   * final Path tested = fs.getPath("/home/joe/myfile");
   *
   * // the following assertion succeeds:
   * assertThat(tested).startsWithRaw(fs.getPath("/home/joe"));
   *
   * // the following assertion fails:
   * assertThat(tested).startsWithRaw(fs.getPath("/home/harry"));
   * // .... and this one too as given path is not canonicalized
   * assertThat(tested).startsWithRaw(fs.getPath("/home/joe/.."));
   * </code></pre>
   *
   * @param other the other path
   * @return self
   *
   * @see Path#startsWith(Path)
   */
  public S startsWithRaw(final Path other) {
	paths.assertStartsWithRaw(info, actual, other);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} ends with the given path.
   *
   * <p>
   * This assertion will attempt to canonicalize the tested path and normalize the path given as an argument before
   * performing the actual test.
   * </p>
   *
   * <p>
   * Note that the criterion to determine success is determined by the path's name elements; therefore,
   * {@code /home/foobar/baz} does <em>not</em> end with {@code bar/baz}.
   * </p>
   *
   * <p>
   * Examples:
   * </p>
   *
   * <pre><code class="java">
   * // fs is a Unix filesystem.
   * // the current directory is supposed to be /home.
   * final Path tested = fs.getPath("/home/joe/myfile");
   * // as tested will be canonicalized, it could have been written: /home/jane/../joe/myfile
   *
   * // the following assertion succeeds:
   * assertThat(tested).endsWith(fs.getPath("joe/myfile"));
   *
   * // the following assertions fail:
   * assertThat(tested).endsWith(fs.getPath("joe/otherfile"));
   * // this path will be normalized to joe/otherfile
   * assertThat(tested).endsWith(fs.getPath("joe/myfile/../otherfile"));
   * </code></pre>
   *
   * @param other the other path
   * @return self
   *
   * @throws PathsException failed to canonicalize the tested path (see class
   *           description)
   * 
   * @see Path#endsWith(Path)
   * @see Path#toRealPath(LinkOption...)
   */
  public S endsWith(final Path other) {
	paths.assertEndsWith(info, actual, other);
	return myself;
  }

  /**
   * Assert that the tested {@link Path} ends with the given path.
   *
   * <p>
   * <em>This assertion will not perform any canonicalization (on the
   * tested path) or normalization (on the path given as an argument); see the
   * class description for more details. If this is not what you want, use
   * {@link #endsWith(Path)} instead.</em>
   * </p>
   *
   * <p>
   * This may lead to some surprising results; for instance, path {@code /home/foo} does <em>not</em> end with
   * {@code foo/.} since the last name element of the former ({@code foo}) is different from the last name element of
   * the latter ({@code .}).
   * </p>
   *
   * <p>
   * Examples:
   * </p>
   *
   * <pre><code class="java">
   * // fs is a Unix filesystem
   * // the current directory is supposed to be /home.
   * final Path tested = fs.getPath("/home/joe/myfile");
   *
   * // The following assertion succeeds:
   * assertThat(tested).endsWithRaw(fs.getPath("joe/myfile"));
   *
   * // But the following assertion fails:
   * assertThat(tested).endsWithRaw(fs.getPath("harry/myfile"));
   * // and this one too as the given path is not normalized
   * assertThat(tested).endsWithRaw(fs.getPath("harry/../joe/myfile"));
   * </code></pre>
   *
   * @param other the other path
   * @return self
   * 
   * @see Path#endsWith(Path)
   */
  public S endsWithRaw(final Path other) {
	paths.assertEndsWithRaw(info, actual, other);
	return myself;
  }
}