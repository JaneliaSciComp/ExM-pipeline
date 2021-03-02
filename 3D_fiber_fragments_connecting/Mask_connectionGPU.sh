#!/bin/bash

smallmaskpath=$1
connectionVX=$2
connectiontime=$3


FIJI="/nrs/scicompsoft/otsuna/Macros/Fiji_Linux.app/ImageJ-linux64"

CMTK="/Applications/FijizOLD.app/bin/cmtk"
MACRO="/nrs/scicompsoft/otsuna/Macros/ExM_expand/Mask_connectionGPU.ijm"


errorfile=${smallmaskpath%.*}"_fail.txt"

echo "smallmaskpath; "${smallmaskpath}


if [[ ! -e $FIJI ]]; then
  echo "no Fiji; "$FIJI
fi

cd $BaseDir

echo " "

#if [[ -e $errorfile ]]; then
#rm -rf $errorfile

if [[ -e $smallmaskpath ]]; then
  $FIJI --headless -macro ${MACRO} "${smallmaskpath},${connectionVX},${connectiontime}"
fi
#fi

num=0
while [ -e $errorfile ]
do
  rm -rf $errorfile

  $FIJI --headless -macro ${MACRO} "${smallmaskpath},${connectionVX},${connectiontime}"
  num=$((num+1))

  if [[ $num == 20 ]]; then
    break
  fi
done



