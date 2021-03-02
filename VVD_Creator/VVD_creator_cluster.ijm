
run("Misc...", "divide=Infinity save");

ResizingSteps=5;//"Resizing steps; number of pyramid LV"
Zresize=1;
Z_resie=true;
XYendratio=10;// % "Smallest resizing % for smallest resolution"
blockposition=1;
totalblock=1;
CPUno=32;//CPU number"
Pyramid=true;//false;//"Pyramid creation"
VVD=true;//"VVD file creation"
PyramidDeletion=true;//delete pyramid after VVD creation
XBrick=512;//"Xbrick size"
YBrick=512;//"Ybrick size"
ZBrick=360;//ZbrickoriginalWidth
CHno=1;//"number of channel"
renameOn=true;
Compression="ZLIB";//"RAW", "ZLIB", "JPEG (8bit only)", "h5j"

EightConv=true;
duplication=1;
Minthre=100;
Maxthre=2100;// for 8bit conversion from 16bit

vvdexportmethod="PackedVVD";//,"ManyVVD","VirtualHDD";//"VirtualHDD (saves memory, but slow)"

setBatchMode(true);

testarg=0;

//testarg="/nrs/scicompsoft/otsuna/EM_test/ExM/Misha/Misha_VVD_ch0/,/nrs/scicompsoft/otsuna/EM_test/ExM/Pyramid/,/nrs/scicompsoft/otsuna/EM_test/ExM/Misha/ch0/,32,5,10"

//testarg="/Volumes/otsuna/EM_test/ExM/Misha/Misha_VVD_ch0/,/Volumes/otsuna/EM_test/ExM/Pyramid/,/Volumes/otsuna/EM_test/ExM/Misha/ch0/,8,5,10"
//testarg="/Users/otsunah/test/VVD_test/3ch/,/test/VVD_test/VVDfile/,11,5,10,200,2000"

if(testarg==0)
args = split(getArgument(),",");
else
args = split(testarg,",");

dir=args[0];//Directory for 2D original tiff
workdir=args[1];// working dir
CPUno=args[2];// CPU number
CPUno=round(CPUno);
ResizingSteps=args[3];// Pyramid level
XYendratio=args[4];// minimum % of smallest resolution

Minthre=args[5];
Minthre=round(Minthre);

Maxthre=args[6];
Maxthre=round(Maxthre);

ResizingSteps=round(ResizingSteps);
XYendratio=round(XYendratio);


dirname=substring(dir, 0, lengthOf(dir)-1);
dirFileIndex=lastIndexOf(dirname,"/");
dirname=substring(dirname, dirFileIndex+1, lengthOf(dirname));

print("dir; "+dir);
print("workdir; "+workdir);
print("dirname; "+dirname);
print("Minthre; "+Minthre+"   Maxthre; "+Maxthre);

workExt=File.exists(workdir);

if(workExt!=1)
File.makeDirectory(workdir);

workExt=File.exists(workdir);
if(workExt!=1)
exit("cannot make folder; "+workdir);

EightDir=workdir+dirname+"_8bit_files"+File.separator;
File.makeDirectory(EightDir);

VVDdir=workdir+dirname+"_VVD"+File.separator;
savedir=workdir+dirname+"_pyramid"+File.separator;

print("VVDdir; "+VVDdir);
print("savedir; "+savedir);
print("EightDir; "+EightDir);

logsum=getInfo("log");
filepath=workdir+"VVD_log_"+dirname+".txt";
File.saveString(logsum, filepath);



VVDdirExt=File.exists(VVDdir);
if(VVDdirExt!=1)
File.makeDirectory(VVDdir);

savedirExt=File.exists(savedir);
if(savedirExt!=1)
File.makeDirectory(savedir);

list2D = getFileList(dir);
Array.sort(list2D);
print("2D tiff; "+dir);
print("EightConv; "+EightConv);

listsave = getFileList(savedir);
Array.sort(listsave);
print("Pyramid; "+savedir);

if(VVD==true){
	print("VVD dir; "+VVDdir);
}
print("CPUno; "+CPUno);
print("XYendratio; "+XYendratio+"   ResizingSteps; "+ResizingSteps+"  CPU Num; "+CPUno);

logsum=getInfo("log");
File.saveString(logsum, filepath);

/// 8bit conversion /////////////////////////////





dirnameST="";
CHindex=indexOf(dirname, "ch");
if(CHindex!=-1)
dirnameST=dirname;
print("dirnameST; "+dirnameST);

logsum=getInfo("log");
File.saveString(logsum, filepath);



preParents = substring(dir, 0, dirFileIndex);

StartParentsIndex = lastIndexOf(preParents, "/");

ParentsDirName=substring(dir, StartParentsIndex+1, dirFileIndex);
print("ParentsDirName; "+ParentsDirName);

logsum=getInfo("log");
File.saveString(logsum, filepath);

realstart=getTime();

run("Close All");
operationNum=0;

endlist=list2D.length;
endlist=round(endlist);
print("endlist; "+endlist+"  list2D[3]"+list2D[3]);

if(EightConv==true){// duplication for 8bit conversion
	a8bit=getTime();
	
	useplugin = true;
	
	if(useplugin==true){
		run("AtmicInteger 8bitconv", "export=Tif lowest="+Minthre+" highest="+Maxthre+" parallel="+CPUno+" choose="+dir+" save="+EightDir+"");//"Nrrd_uncompressed" "Tif" "Compressed_Nrrd"
		
	}//	if(EightConv==true){
	
	if(useplugin!=true){
		
		for(i=0; i<endlist; i++){
			//	print("list2D[i]; "+list2D[i]);
			operationNum=operationNum+1;
			
			if(operationNum==50){
				for (trials=0; trials<3; trials++) {
					call("java.lang.System.gc");
					wait(100);
				}
				operationNum=0;
			}
			
			dotindextif = -1;
			dotindextif = lastIndexOf(list2D[i], ".tif");
			
			dotIndexLSM = -1;
			dotIndexLSM = lastIndexOf(list2D[i], ".lsm");
			
			dotIndexnrrd = -1;
			dotIndexnrrd = lastIndexOf(list2D[i], ".nrrd");
			if(dotIndexnrrd==-1)
			dotIndexnrrd = lastIndexOf(list2D[i], ".v3draw");
			
			dotIndexh5j = -1;
			dotIndexh5j = lastIndexOf(list2D[i], ".h5j");
			
			dotIndexzip = -1;
			dotIndexzip = lastIndexOf(list2D[i], ".zip");
			
			dotIndexavi = -1;
			dotIndexavi = lastIndexOf(list2D[i], ".avi");
			
			noext2=0;
			if(dotIndexLSM>-1 || dotIndexzip>0 || dotindextif>-1 || dotIndexnrrd>-1 || dotIndexavi>-1 || dotIndexh5j>-1){
				path=dir+list2D[i];
				
				if(dirnameST!=""){
					chPositive = indexOf(list2D[i], "ch");
					
					if(chPositive!=-1)
					dirnameST="";
				}
				
				open(path);
				print(path+"  "+i+" / "+endlist);
				
				dotindex=lastIndexOf(list2D[i], ".");
				truename=substring(list2D[i], 0,dotindex);
				
				origi=getTitle();
				
				bitd=bitDepth();
				getDimensions(width, height, channels, slices, frames);
				getVoxelSize(VxWidth, VxHeight, VxDepth, VxUnit);
				//		print("bitd; "+bitd+"   channels; "+channels);
				
				logsum=getInfo("log");
				File.saveString(logsum, filepath);
				
				if(bitd==24){
					
					print("24bit mode run");
					run("Split Channels");
					run("Properties...", "channels=1 slices="+nSlices+" frames=1 unit=microns pixel_width="+VxWidth+" pixel_height="+VxHeight+" voxel_depth="+VxDepth+"");
					
					if(isOpen(origi+" (red)"))
					selectWindow(origi+" (red)");
					
					if(isOpen("C1-"+origi))
					selectWindow("C1-"+origi);
					
					run("Nrrd Writer", "compressed nrrd="+EightDir+truename+"_01.nrrd");
					close();
					
					if(isOpen(origi+" (green)"))
					selectWindow(origi+" (green)");
					
					if(isOpen("C2-"+origi))
					selectWindow("C2-"+origi);
					run("Properties...", "channels=1 slices="+nSlices+" frames=1 unit=microns pixel_width="+VxWidth+" pixel_height="+VxHeight+" voxel_depth="+VxDepth+"");
					run("Nrrd Writer", "compressed nrrd="+EightDir+truename+"_02.nrrd");
					close();
				}//if(bitd==24 || channels>1){
				
				if(bitd==16 && EightConv==true){	
					
					logsum=getInfo("log");
					File.saveString(logsum, filepath);
					
					setMinAndMax(Minthre, Maxthre);		
					truenameNum=parseFloat(truename);//Chaneg string to number
					
					if(isNaN(truenameNum)){
						truenameNum=i;
						truename=i;
					}
					
					ZeroST="";
					if(truenameNum<10)
					ZeroST="0000";
					else if(truenameNum<100 && truenameNum>9)
					ZeroST="000";
					else if(truenameNum<1000 && truenameNum>99)
					ZeroST="00";
					else if(truenameNum<10000 && truenameNum>999)
					ZeroST="0";
					
					run("8-bit");
					run("Nrrd ... ", "nrrd="+EightDir+dirnameST+"_"+ZeroST+truename+".nrrd");
					
				}
				
				if(EightConv==false){
					print("EightConv==false");
					if(channels!=1){
						if(bitd==8 || bitd==16){
							
							getVoxelSize(VxWidth, VxHeight, VxDepth, VxUnit);
							if(nSlices>1){
								idealVx=190/((nSlices/channels)-25);
								
								if(VxWidth==1){
									VxWidth=0.62;
									VxHeight=0.62;
								}
								
								run("Properties...", "channels="+channels+" slices="+nSlices/channels+" frames=1 unit=microns pixel_width="+VxWidth+" pixel_height="+VxHeight+" voxel_depth="+idealVx+"");
							}
							run("Split Channels");
							titlelist=getList("image.titles");
							print(titlelist.length);
							selectWindow(titlelist[0]);
							if(nSlices>1){
								run("Z Project...", "projection=[Max Intensity]");
								run("Grays");
								run("Enhance Contrast", "saturated=0.35");
								run("8-bit");
								saveAs("JPEG", ""+savedir+truename+".jpg");//save 20x MIP
								close();
							}
							selectWindow(titlelist[0]);
							run("Nrrd Writer", "compressed nrrd="+EightDir+truename+"_01.nrrd");
							close();
							//		print("221");
							if(isOpen(titlelist[1])){
								selectWindow(titlelist[1]);
								run("Nrrd Writer", "compressed nrrd="+EightDir+truename+"_02.nrrd");
								close();
							}
							if(channels>2){
								if(isOpen(titlelist[2])){
									selectWindow(titlelist[2]);
									run("Nrrd Writer", "compressed nrrd="+EightDir+truename+"_03.nrrd");
									close();
								}
							}
							if(channels>3){
								if(isOpen(titlelist[3])){
									selectWindow(titlelist[3]);
									run("Nrrd Writer", "compressed nrrd="+EightDir+truename+"_04.nrrd");
									close();
								}
							}
							//		print("225");
						}//if(bitd==16 && EightConv==false && channels>1){
					}
				}//	if(EightConv==false){
				
				if(bitd==8 && channels==1){//if(bitd==8 && channels==1)
					print("8bit mode run");
					if(getWidth==1024 && getHeight==512)
					run("Properties...", "channels=1 slices="+nSlices+" frames=1 unit=microns pixel_width=0.62 pixel_height=0.62 voxel_depth=1.0000000");
					
					truenameNum=parseFloat(truename);//Chaneg string to number
					
					ZeroST="";
					if(truenameNum<10)
					ZeroST="0000";
					else if(truenameNum<100 && truenameNum>9)
					ZeroST="000";
					else if(truenameNum<1000 && truenameNum>99)
					ZeroST="00";
					else if(truenameNum<10000 && truenameNum>999)
					ZeroST="0";
					
					run("Nrrd Writer", "compressed nrrd="+EightDir+dirnameST+"_"+ZeroST+truename+".nrrd");
					
					dir=EightDir;
					//		list = getFileList(dir);
					//		Array.sort(list);
				}
				
			}//if(dotIndexLSM>-1 || dotIndexzip>0 || dotindextif>-1 || dotIndexnrrd>-1 || dotIndexavi>-1 || dotIndexh5j>-1){
			if(nImages>0)
			run("Close All");
		}//for(i=0; i<list2D.length; i++){
	}//	if(useplugin!=true){
	b8bit=getTime();
	
	print("8bit conversion; "+(b8bit-a8bit)/1000+" sec");
	
}//if(EightConv==true){

if(EightConv==true){
	dir=EightDir;
	list = getFileList(dir);
	Array.sort(list);
}else{
	
	list = getFileList(dir);
}



Chname=newArray(CHno);
ChStartNum=newArray(CHno);
for(iiname=0; iiname<CHno; iiname++){
	Chname[iiname]=0;
}

print("Universal_Name_Searching.");
logsum=getInfo("log");
File.saveString(logsum, filepath);
extensionN=".nrrd";

UniverseName=""; sliceDigit=0; TiffNumber=1;
for(Ori2Dtiff=0; Ori2Dtiff<list.length-1; Ori2Dtiff++){//Universal ファイルネームを探す。
	isub2=1;NameIndex=-1;
	extensionT=".tif";
	TifIndex=lastIndexOf(list[Ori2Dtiff],".tif");
	if(TifIndex==-1){
		TifIndex=lastIndexOf(list[Ori2Dtiff],".nrrd");
		if(TifIndex!=-1)
		extensionN=".nrrd";
	}
	TifIndex2=lastIndexOf(list[Ori2Dtiff+1],".tif");
	if(TifIndex2==-1)
	TifIndex2=lastIndexOf(list[Ori2Dtiff+1],".nrrd");
	
	if(TifIndex!=-1 && TifIndex2!=-1){
		CurrentTif=substring(list[Ori2Dtiff],0,TifIndex);
		NextTif=substring(list[Ori2Dtiff+1],0,TifIndex2);
		TiffNumber=TiffNumber+1;
		
		previous=1;
		if(previous==1){
			while(NameIndex==-1 && isub2<lengthOf(CurrentTif)/2){//clipping from last position of the file name
				
				for(isub2=0; isub2<lengthOf(CurrentTif)/2; isub2++){
					
					for(iend=lengthOf(CurrentTif); iend>lengthOf(CurrentTif)/2; iend--){
						
						//	print("NameIndex; "+NameIndex+"   isub2; "+isub2+"   iend; "+iend);
						
						CurrentTif3=substring(list[Ori2Dtiff],isub2,iend);
						NextTif=substring(list[Ori2Dtiff+1],isub2,iend);
						
						//	print("CurrentTif3; "+CurrentTif3+"   NextTif; "+NextTif);
						if(NextTif==CurrentTif3){
							
							if(sliceDigit==0){
								ZeroOrNot=1;
								while(ZeroOrNot==1){
									ZeroOrNot=endsWith(CurrentTif3,"0");
									tiflength=lengthOf(CurrentTif3);
									if(ZeroOrNot==1){
										CurrentTif3=substring(CurrentTif3,0,tiflength-1);
										sliceDigit=sliceDigit+1;
									}
								}
							}
							//		print("sliceDigit; "+sliceDigit);
							CurrentTif3=substring(list[Ori2Dtiff],isub2,iend);
							
							if(lengthOf(UniverseName)>lengthOf(CurrentTif3) || lengthOf(UniverseName)==0){
								UniverseName=CurrentTif3;
								print("UniverseName  Ch start number "+Ori2Dtiff+1+"   UniverseName; "+UniverseName);
								logsum=getInfo("log");
								File.saveString(logsum, filepath);
							}
							iend=lengthOf(CurrentTif)/2;
							isub2=lengthOf(CurrentTif)/2;
							NameIndex=1;
						}//if(NextTif==CurrentTif3){
					}
				}
			}//	while(NameIndex==-1 && isub>lengthOf.CurrentTif/2){//clipping from last position of the file name
		}//if(previous==1){
	}//	if(TifIndex!=-1 && TifIndex2!=-1){
}// universal name		

print("Universal Name search finished");
logsum=getInfo("log");
File.saveString(logsum, filepath);

ChannelTiffNo=TiffNumber/CHno;

AddingZeroST="";
if(ChannelTiffNo>999 && sliceDigit==2 && ChannelTiffNo<10000){
	AddingZeroST="0";
}else if(ChannelTiffNo>999 && sliceDigit==1 && ChannelTiffNo<10000){
	AddingZeroST="00";
}else if(ChannelTiffNo>99 && sliceDigit==1 && ChannelTiffNo<1000){
	AddingZeroST="0";
}
//sliceDigit=sliceDigit+1;

ChNum=0;StartTif=-1; noRename=0;
for(Ori2Dtiff=0; Ori2Dtiff<list.length-1; Ori2Dtiff++){
	
	//	print("Chname[ChNum]; "+Chname[ChNum]+"  Try; "+Ori2Dtiff);
	//	print(" ");
	if(Chname[ChNum]!=0){
		
		tifIndex=indexOf (list[Ori2Dtiff+1],".tif");
		if(tifIndex==-1)
		tifIndex=lastIndexOf(list[Ori2Dtiff],".nrrd");
		
		NameIndexCH=indexOf (list[Ori2Dtiff+1],Chname[ChNum]);//Chname[ChNum] is CHn file name
		if(NameIndexCH==-1 && tifIndex!=-1){
			
			if(ChNum+1==CHno){
				noRename=1;
				//exit("This is not "+CHno+" channels folder, detected "+ChNum+2+" channels");
				Ori2Dtiff=list.length;
				
				print("noRename; "+noRename);
				logsum=getInfo("log");
				File.saveString(logsum, filepath);
				
			}
			if(noRename==0){
				ChNum=ChNum+1;// go to next channel
				ChStartNum[ChNum]=Ori2Dtiff+1;//ch n start number
				print("Ch"+ChNum+1+" start number; "+Ori2Dtiff+1);
			}
		}
		
		
	}else if(Chname[ChNum]==0){//もし、現状、次のtiffが一致しなかったら
		TifIndex=lastIndexOf(list[Ori2Dtiff],".tif");
		if(TifIndex==-1)
		TifIndex=lastIndexOf(list[Ori2Dtiff],".nrrd");
		
		TifIndex2=lastIndexOf(list[Ori2Dtiff+1],".tif");
		if(TifIndex2==-1)
		TifIndex2=lastIndexOf(list[Ori2Dtiff+1],".nrrd");
		
		if(TifIndex!=-1){
			
			if(StartTif==-1){
				StartTif=Ori2Dtiff;
				ChStartNum[ChNum]=Ori2Dtiff;//ch1 start number
				print("Ch1 start number; "+Ori2Dtiff);
			}
			
			if(TifIndex2!=-1){
				CurrentTif=substring(list[Ori2Dtiff],0,TifIndex);
				NextTif=substring(list[Ori2Dtiff+1],0,TifIndex2);
				
				if(CurrentTif!=NextTif){
					
					NameIndex=-1; isub=lengthOf(CurrentTif); endValue=lengthOf(CurrentTif)/2;
					while(NameIndex==-1 && isub>endValue){//clipping from last position of the file name
						
						for(isub=lengthOf(CurrentTif); isub>sliceDigit; isub--){
							CurrentTif2=substring(list[Ori2Dtiff],0,isub);
							NameIndex=lastIndexOf(NextTif,CurrentTif2);
							
							if(NameIndex!=-1){
								CurrentTif2=substring(list[Ori2Dtiff],0,isub-sliceDigit);//name of each channel, except z-number
								Chname[ChNum]=CurrentTif2;
								print("Chname[ChNum]; Ch; "+ChNum+"  "+Chname[ChNum]);
								print(" ");
								isub=sliceDigit;
							}
						}//	for(isub=lengthOf.CurrentTif; isub>0; isub--){
					}//	while(NameIndex==-1 && isub>lengthOf.CurrentTif/2){//
				}//if(CurrentTif!=NextTif){
			}
		}
	}//if(Chname[ChNum]==0){
}//for(Ori2Dtiff=0; Ori2Dtiff<list.length; Ori2Dtiff++){

GoRename=0;endTif=0;
if(CHno==3){
	endCHratio=ChStartNum[2]/list.length;
	if(endCHratio>0.5 && endCHratio<0.7){
		GoRename=1;
		endTif=TiffNumber/3;
	}else
	print("no rename perfomed");
	//	exit("This is not 3 channels folder, cannot rename");
	
}else if(CHno==2){
	endCHratio=ChStartNum[1]/list.length;
	if(endCHratio>0.45 && endCHratio<0.55){
		GoRename=1;
		endTif=TiffNumber/2;
	}else
	print("no rename perfomed");
	//	exit("This is not 2 channels folder, cannot rename");
}

print("noRename; "+noRename);
print("endTif; "+endTif);
logsum=getInfo("log");
File.saveString(logsum, filepath);

//equalize z digit /////////////////

if(noRename==0){
	maxZlength0=0; maxZlength1=0; maxZlength2=0;
	for(iequalize=1; iequalize<=endTif; iequalize++){
		
		tifIndex0=lastIndexOf(list[iequalize-1+ChStartNum[0]],".");
		if(CHno==2 || CHno==3)
		tifIndex1=lastIndexOf(list[iequalize-1+ChStartNum[1]],".");
		if(CHno==3)
		tifIndex2=lastIndexOf(list[iequalize-1+ChStartNum[2]],".");
		
		if(tifIndex0!=-1)
		NumST0=substring(list[iequalize-1+ChStartNum[0]], lengthOf(Chname[0]), tifIndex0);
		if(CHno==2 || CHno==3)
		NumST1=substring(list[iequalize-1+ChStartNum[1]], lengthOf(Chname[1]), tifIndex1);
		if(CHno==3)
		NumST2=substring(list[iequalize-1+ChStartNum[2]], lengthOf(Chname[2]), tifIndex2);
		
		length0=lengthOf(NumST0);
		if(CHno==2 || CHno==3)
		length1=lengthOf(NumST1);
		if(CHno==3)
		length2=lengthOf(NumST2);
		
		if(maxZlength0<length0)
		maxZlength0=length0;
		
		if(CHno==2 || CHno==3){
			if(maxZlength1<length1)
			maxZlength1=length1;
		}
		if(CHno==3){
			if(maxZlength2<length2)
			maxZlength2=length2;
		}
	}//for(iequalize=1; iequalize<=endTif; iequalize++){
	
	print("maxZlength0; "+maxZlength0+"  maxZlength1; "+maxZlength1+"   AddingZeroST; "+AddingZeroST+"  ChannelTiffNo; "+ChannelTiffNo);
	
	logsum=getInfo("log");
	File.saveString(logsum, filepath);
	
	for(iequalize2=1; iequalize2<=endTif; iequalize2++){
		
		tifIndex0=lastIndexOf(list[iequalize2-1+ChStartNum[0]],".");
		if(CHno==2 || CHno==3)
		tifIndex1=lastIndexOf(list[iequalize2-1+ChStartNum[1]],".");
		if(CHno==3)
		tifIndex2=lastIndexOf(list[iequalize2-1+ChStartNum[2]],".");
		
		NumST0=substring(list[iequalize2-1+ChStartNum[0]], lengthOf(Chname[0]), tifIndex0);
		if(CHno==2 || CHno==3)
		NumST1=substring(list[iequalize2-1+ChStartNum[1]], lengthOf(Chname[1]), tifIndex1);
		if(CHno==3)
		NumST2=substring(list[iequalize2-1+ChStartNum[2]], lengthOf(Chname[2]), tifIndex2);
		
		//	print("NumST0; "+NumST0);
		
		length0=lengthOf(NumST0);
		if(CHno==2 || CHno==3)
		length1=lengthOf(NumST1);
		if(CHno==3)
		length2=lengthOf(NumST2);
		
		if(maxZlength0>length0)
		if(lastIndexOf(list[iequalize2-1+ChStartNum[0]],extensionN)!=-1)
		extension=extensionN;
		else if(lastIndexOf(list[iequalize2-1+ChStartNum[0]],extensionT)!=-1)
		extension=extensionT;
		
		File.rename(dir+list[iequalize2-1+ChStartNum[0]], dir+Chname[0]+AddingZeroST+NumST0+extension); // - Renames, or moves, a file or directory. Returns "1" (true) if successful. 
		
		if(CHno==2 || CHno==3){
			if(maxZlength1>length1)
			File.rename(dir+list[iequalize2-1+ChStartNum[1]], dir+Chname[1]+AddingZeroST+NumST1+extension);
		}
		if(CHno==3){
			if(maxZlength2>length2)
			File.rename(dir+list[iequalize2-1+ChStartNum[2]], dir+Chname[2]+AddingZeroST+NumST2+extension);
		}
	}
}//if(noRename==0){

//run("Quit");

list = getFileList(dir);
Array.sort(list);
//	for(ii=0; ii<list.length; ii++){
//		print(list[ii]);
//	}

if(GoRename==1 && CHno>1){	//rename start
	irenameChNum=1;
	for(irename=1; irename<=endTif; irename++){
		
		
		if(irename<10000 && irename>999)
		digit="";
		
		else if(irename<1000 && irename>99)
		digit="0";
		
		else if(irename>9 && irename<100)
		digit="00";
		
		if(irename<10)
		digit="000";
		
		
		if(lastIndexOf(list[irename-1+ChStartNum[0]],extensionN)!=-1)
		extension=extensionN;
		else if(lastIndexOf(list[irename-1+ChStartNum[0]],extensionT)!=-1)
		extension=extensionT;
		
		File.rename(dir+list[irename-1+ChStartNum[0]], dir+"Z"+digit+irename+"_"+Chname[0]+extension); // - Renames, or moves, a file or directory. Returns "1" (true) if successful. 
		File.rename(dir+list[irename-1+ChStartNum[1]], dir+"Z"+digit+irename+"_"+Chname[1]+extension);
		if(CHno==3)
		File.rename(dir+list[irename-1+ChStartNum[2]], dir+"Z"+digit+irename+"_"+Chname[2]+extension);
		
	}
}//	if(GoRename==1){	//rename start
//}



print("Rename finish"+"   ResizingSteps; "+ResizingSteps+"   XYendratio; "+XYendratio);
logsum=getInfo("log");
File.saveString(logsum, filepath);

StepRatioArray=newArray(ResizingSteps);

print("624");
logsum=getInfo("log");
File.saveString(logsum, filepath);

xySizeRatioArray=newArray(ResizingSteps);
xySizeRatio=XYendratio;
gapRatio=100-XYendratio;//80

print("632"+"   gapRatio; "+gapRatio);
logsum=getInfo("log");
File.saveString(logsum, filepath);

StepRatio=(gapRatio/(ResizingSteps-1));

print("638  StepRatio; "+StepRatio);
logsum=getInfo("log");
File.saveString(logsum, filepath);

setResult("%", 0, XYendratio);

print("639");
logsum=getInfo("log");
File.saveString(logsum, filepath);

setResult("Multiply", 0, 100/XYendratio);

print("XYendratio; "+XYendratio+"   xySizeRatio; "+xySizeRatio+"   StepRatio; "+StepRatio);
logsum=getInfo("log");
File.saveString(logsum, filepath);

for(iistep=1; iistep<ResizingSteps; iistep++){
	
	xySizeRatioArray[iistep-1]=xySizeRatio;
	xySizeRatio=xySizeRatio+StepRatio;
	setResult("%", iistep, xySizeRatio);
	setResult("Multiply", iistep, 100/xySizeRatio);
}
updateResults();
setBatchMode(true);

print("Pyramid creation start");
logsum=getInfo("log");
File.saveString(logsum, filepath);

if(Pyramid==true){
	a=getTime();
	//run("AtmicInteger SingleFolderSave", "final=20 pyramid=5 parallel=8 channel=3 choose=/test/VVD_test/ch0 save=/test/VVD_test");
	//	print(dir);
	
	//Atmicdir=substring(dir, 0, 	lengthOf(dir)-1);
	//AtmicSavedir=substring(savedir, 0, 	lengthOf(savedir)-1);
	run("AtmicInteger Single FolderSave", "final="+XYendratio+" pyramid="+ResizingSteps+" parallel="+CPUno+" channel="+CHno+" choose=["+dir+"] save=["+savedir+"]");	
	b=getTime();
	print((b-a)/1000/60+"min for pyramid creation");
	
	
	a=getTime();
	for(zi2=1; zi2<ResizingSteps; zi2++){
		dirsub=savedir+"resized"+zi2+File.separator;
		
		Zsize=getResult("Multiply", zi2-1);
		
		if(Zsize>=2){
			Zsize=round(Zsize);	
			run("AtmicInteger MIP", "slice="+Zsize+" parallel="+CPUno+" delete choose=["+dirsub+"] save=["+dirsub+"]");
		}
	}
	b=getTime();
	
	gap=b-a;
	print(gap/1000/60+" min for Z-resizing");
}

logsum=getInfo("log");
File.saveString(logsum, filepath);

//run("Quit");

call("java.lang.System.gc");
if(VVD==true){
	//open tiff as virtual stack
	//	print("VVD folder; "+VVDdir);
	listsave = getFileList(savedir);
	Array.sort(listsave);
	open(dir,"virtual");
	Original=getTitle();
	OrigiID=getImageID();
	bitd=bitDepth();
	
	if(Compression=="JPEG (8bit only)" && bitd==16){
		
	}
	
	//	print("Original 2D tiff open");
	if(CHno>1)
	run("Stack to Hyperstack...", "order=xyczt(default) channels="+CHno+" slices="+nSlices/CHno+" frames=1 display=Composite");
	
	getDimensions(originalWidth, originalHeight, channels, originalSlices, frames);
	
	imagesize = round((originalWidth*originalHeight*originalSlices)/1000000);
	print("Original imagesize; "+imagesize+"  MB");
	
	TitleArray=newArray(ResizingSteps);
	resizeOpen=1;
	
	ZBrickArray=newArray(ResizingSteps-1);
	//	print("listsave.length "+listsave.length);
	
	smallstackgeneration=1;
	
	if(smallstackgeneration==1){
		open(savedir+"resized1"+File.separator,"virtual");
		
		saveAs("ZIP", ""+workdir+dirname+"_small.zip");
		close();
	}
	
	for(TwoDtiff=0; TwoDtiff<listsave.length; TwoDtiff++){
		if(endsWith (listsave[TwoDtiff],"/")){
			
			resizeIndex=lastIndexOf(listsave[TwoDtiff],"resized");
			if(resizeIndex!=-1){
				
				subdir2=savedir+"resized"+resizeOpen+File.separator;
				listsub=getFileList(subdir2);
				
				subtifNum=0;
				for(isub=0; isub<listsub.length; isub++){
					subtif=indexOf(listsub[isub],extensionT);
					if(subtif==-1)
					subtif=indexOf(listsub[isub],extensionN);
					
					if(subtif!=-1)
					subtifNum=subtifNum+1;
				}
				print("subtifNum; "+subtifNum+"   resizeOpen; "+resizeOpen+"   ZBrick; "+ZBrick);
				
				if(subtif!=-1){
					if(subtifNum<ZBrick)
					ZBrickArray[resizeOpen-1]=subtifNum;
					else
					ZBrickArray[resizeOpen-1]=ZBrick;
					
					open(savedir+"resized"+resizeOpen+File.separator,"virtual");
					print("resized"+resizeOpen+ " open");
					if(CHno>1)
					run("Stack to Hyperstack...", "order=xyczt(default) channels="+CHno+" slices="+nSlices/CHno+" frames=1 display=Composite");
					
					Zsize=getResult("Multiply", resizeOpen-1);
					
					
					if(Zsize>=2){
						Zsize=round(Zsize);	
						getVoxelSize(VxWidth, VxHeight, VxDepth, VxUnit);
						setVoxelSize(VxWidth, VxHeight, Zsize, VxUnit);//change xyz voxel size
					}
					resizeOpen=resizeOpen+1;
				}//if(subtif!=-1){
			}
		}
	}
	
	BasicST="brickwidth="+XBrick+" brickheight="+YBrick+" brickdepth="+ZBrick+" levels="+resizeOpen+" filetype="+Compression+" thread="+CPUno+" image0=["+Original+"] bricksize0=["+XBrick+" "+YBrick+" "+ZBrick+"] ";
	String.resetBuffer;
	String.append(BasicST);
	
	logsum=getInfo("log");
	File.saveString(logsum, filepath);
	
	//crate VVD file
	for(VVDlvadd=1; VVDlvadd<=resizeOpen-1; VVDlvadd++){
		print("Zbrick size; "+ZBrickArray[ZBrickArray.length-VVDlvadd]);
		addST="image"+VVDlvadd+"=resized"+resizeOpen-VVDlvadd+" bricksize"+VVDlvadd+"=["+XBrick+" "+YBrick+" "+ZBrickArray[ZBrickArray.length-VVDlvadd]+"] ";
		String.append(addST);
	}
	TotalST=String.buffer;
	print("resizeOpen; "+resizeOpen+"   ;"+TotalST);
	a=getTime();
	
	if(vvdexportmethod=="ManyVVD")
	run("vvd export", TotalST+"save=["+VVDdir+dirname+"]");
	else if(vvdexportmethod=="VirtualHDD")
	run("vvd export for large data", TotalST+"choose=["+savedir+"] save=["+VVDdir+dirname+"]");
	else if(vvdexportmethod=="PackedVVD")
	run("vvd export pack", TotalST+"save=["+VVDdir+dirname+"]");
	
	b=getTime();
	gap=b-a;
	print(gap/1000/60+" min for VVD_export");
	
	logsum=getInfo("log");
	File.saveString(logsum, filepath);
	
	run("Close All");
	call("java.lang.System.gc");
	if(PyramidDeletion==true){
		a=getTime();
		for(pyramidFolder=0; pyramidFolder<listsave.length; pyramidFolder++){
			if(endsWith (listsave[pyramidFolder],File.separator)){
				resizeIndex=lastIndexOf(listsave[pyramidFolder],"resized");
				if(resizeIndex!=-1){
					resizedList=getFileList(savedir+listsave[pyramidFolder]);
					
					for(idel=0; idel<resizedList.length; idel++){
						File.delete(savedir+listsave[pyramidFolder]+resizedList[idel]);	
					}
					File.delete(savedir+listsave[pyramidFolder]);	
				}//if(resizeIndex!=-1){
			}
		}//for(pyramidFolder=0; pyramidFolder<listsave; pyramidFolder++){
		File.delete(savedir);	
		b=getTime();
		gap=b-a;
		print(gap/1000/60+" min for Pyramids deletion");
		
		EightExt=File.exists(EightDir);
		if(EightExt==1){
			listEight = getFileList(EightDir);
			Array.sort(listEight);
			for(EightDel=0; EightDel<listEight.length; EightDel++){
				File.delete(EightDir+listEight[EightDel]);	
			}
			File.delete(EightDir);
		}//if(EightExt==1){
	}//	if(PyramidDeletion==true){
}

realend=getTime();

print((realend-realstart)/1000/60+" min for whole processing");

"Done"

logsum=getInfo("log");
File.saveString(logsum, filepath);

run("Misc...", "divide=Infinity save");
run("Quit");






