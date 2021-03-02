#!/bin/bash

inputdir=$1
outdir=$2
cpunum=$3


FIJI="/groups/scicompsoft/home/otsunah/Desktop/Fiji.app/ImageJ-linux64"
MACRO="/nrs/scicompsoft/otsuna/Macros/ExM_expand/ExpandMask_ExM.ijm"


#errorfile=${inputdir%.*}"_fail.txt"

echo "inputdir; "${inputdir}
echo "outdir; "${outdir}
echo "cpunum; "${cpunum}


if [[ ! -e $FIJI ]]; then
  echo "no Fiji; "$FIJI
fi


if [[ -e $inputdir ]]; then
$FIJI --headless -macro ${MACRO} "${inputdir},${outdir},${cpunum}"
fi





