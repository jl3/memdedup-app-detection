#!/bin/bash

mkdir bins

files=`ls *.deb`

for file in $files
do
	version=$file
	version=${version/apache2-bin_/}
	version=${version/apache2.2-bin_/}
	version=${version/_amd64.deb/}
	echo $version
	mkdir bins/$version
	dpkg --fsys-tarfile $file | tar xOf - ./usr/sbin/apache2 > bins/$version/apache2
done
