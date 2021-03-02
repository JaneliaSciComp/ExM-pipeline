#sh wrote by Hideo Otsuna

FIJI=/groups/scicompsoft/home/otsunah/Desktop/Fiji.app/ImageJ-linux64

PREPROCIMG=/nrs/scicompsoft/otsuna/EM_test/ExM/VVD_creator_cluster.ijm

Originaldir=$1
workdir=$2
PyramidLV=$3
FinalRatio=$4
MinThre=$5
MaxThre=$6


if [ ! -e $Pyramiddir]
then
mkdir $Pyramiddir
fi

if [ ! -e $VVDdir]
then
mkdir $VVDdir
fi

#number of CPU
numCPU=32


cd $Originaldir
#$FIJI -macro $PREPROCIMG "$VVDdir/,$Pyramiddir/,$Originaldir/,$numCPU,$PyramidLV,$FinalRatio"
$FIJI -macro $PREPROCIMG "$Originaldir/,$workdir/,$numCPU,$PyramidLV,$FinalRatio,$MinThre,$MaxThre"

exit 1
