#!/bin/bash

CPUnum=$1
inputdir=$2
outputdir=$3

FIJI="/groups/scicompsoft/home/otsunah/Desktop/Fiji.app/ImageJ-linux64"

CMTK="/Applications/FijizOLD.app/bin/cmtk"
MACRO="/nrs/scicompsoft/otsuna/Macros/MIP_multi/MIPmultithread.ijm"

echo "CPUnum; "${CPUnum}
echo "inputdir"${inputdir}
echo "outputdir"${outputdir}


echo " "

if [[ -e $inputdir ]]; then
$FIJI --headless -macro ${MACRO} "${CPUnum},${inputdir},${outputdir}"
fi




