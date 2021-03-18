#!/bin/bash

saveformat=$1
CPUnum=$2
startslice=$3
endslice=$4
inputdir=$5
outputdir=$6
ROIfullfilepath=$7


FIJI="/groups/scicompsoft/home/otsunah/Desktop/Fiji.app/ImageJ-linux64"

MACRO="/nrs/scicompsoft/otsuna/Macros/multi_threshold/ROI_Crop_multithread.ijm"

echo "CPUnum; "${CPUnum}
echo "inputdir"${inputdir}
echo "outputdir"${outputdir}


echo " "

if [[ -e $inputdir ]]; then
$FIJI --headless -macro ${MACRO} "${saveformat},${CPUnum},${startslice},${endslice}${inputdir},${outputdir},${ROIfullfilepath}"
fi




