#!/bin/bash

CPUnum=$1
inputdir=$2
outputdir=$3
threval=$4

#FIJI="/groups/scicompsoft/home/otsunah/Desktop/Fiji.app/ImageJ-linux64"
FIJI="/nrs/scicompsoft/otsuna/Macros/Fiji.appold/ImageJ-linux64"


CMTK="/Applications/FijizOLD.app/bin/cmtk"
MACRO="/nrs/scicompsoft/otsuna/Macros/multi_threshold/thresholding_multithread.ijm"

echo "CPUnum; "${CPUnum}
echo "inputdir"${inputdir}
echo "outputdir"${outputdir}
echo "threval; "${threval}

echo " "

if [[ -e $inputdir ]]; then
$FIJI --headless -macro ${MACRO} "${CPUnum},${inputdir},${outputdir},${threval}"
fi




