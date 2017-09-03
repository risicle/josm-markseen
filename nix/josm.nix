{ fetchsvn, stdenv, antBuild }:

antBuild rec {
  name = "josm-${version}";
  version = "12545";

  src = fetchsvn rec {
    name = "josm-r${rev}";
    url = "https://josm.openstreetmap.de/svn/trunk";
    sha256 = "1f9w8dsmh79gpzdglmh4vnpkmdk1nx4f4k80qvsz4sbf7b8yy8sd";
    rev=version;
  };

  patches = [
    (
      if
        (builtins.compareVersions version "12620") == -1
      then
        ./josm-build-without-javafx-pre-r12620.patch
      else
        ./josm-build-without-javafx-r12620-onwards.patch
    )
  ];

  antTargets = [
    "dist"
    "test-compile"
  ];
  antProperties = [
    { name = "version"; value = version; }
  ] ++ stdenv.lib.optional stdenv.isLinux { name = "noJavaFX"; value = "true"; };
  jars = [
    "dist/josm-custom.jar"
  ];

  outputs = [ "out" "testBuildDir" ];

  postInstall = ''
    cp -r test/build $testBuildDir
  '';

  meta = with stdenv.lib; {
    description = "An extensible editor for ​OpenStreetMap";
    homepage = https://josm.openstreetmap.de/;
    license = licenses.gpl2Plus;
    platforms = platforms.all;
  };
}