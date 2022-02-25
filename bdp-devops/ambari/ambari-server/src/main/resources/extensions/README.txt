Extensions should include a folder with the extension name.
Subfolders of the extension name folder represents different
extension versions.

For a sample extension MY_EXT 1.0, I would create subfolders: MY_EXT/1.0

Within each extension version folder, there should be both a metainfo.xml
file and a services folder.  The metainfo.xml should contain the
stack versions with which the extension version are compatible.

For example the following metainfo.xml shows an extension that is
compatible with both HDP 2.4 and HDP 2.5:

<metainfo>
  <prerequisites>
    <min-stack-versions>
      <stack>
        <name>HDP</name>
        <version>2.4</version>
      </stack>
      <stack>
        <name>HDP</name>
        <version>2.5</version>
      </stack>
    </min-stack-versions>
  </prerequisites>
</metainfo>

The services folder will contain all services that are part of the
extension version.  The contents of those service folders will be the
same as what you would find in under a stack version's services folder.
