
run("Misc...", "divide=Infinity save");
List.clear();
setBatchMode(true);

testArg=0;

//testArg="/Guest/josh/22t_small.zip,/Guest/josh/smooth_packbits,15932,8316,8518,10,11";

//testArg="H:\\josh_exm\\22t_small.zip,H:\\josh_exm\\out,15932,8316,8518,10,15,29";

if(testArg!=0)
args = split(testArg,",");
else
args = split(getArgument(),",");

input = args[0];// small mask.tif
connectionVX = args[1];
connectiontime = args[2];

dotIndexend = lastIndexOf(input, ".tif");
if(dotIndexend==-1)
dotIndexend = lastIndexOf(input, ".zip");

print("input; "+input);
print("connectionVX; "+connectionVX);
print("connectiontime; "+connectiontime);


if(dotIndexend!=-1){	
	open(input);
	
	success=0; failnum=300;

	mingaptime=(8000/400)*nSlices;
	
	for(iconnect=0; iconnect<connectiontime; iconnect++){
		start=getTime();
		run("Connect Flagments", "radius="+connectionVX+"");
		
		end=getTime();
		gap=end-start;
		
		num=0;
		while(gap<mingaptime){
			wait(600);
			start=getTime();
			run("Connect Flagments", "radius="+connectionVX+"");
			
			end=getTime();
			gap=end-start;
			
			print(iconnect+"-num; "+num+"  gap; "+gap+" ms");
			num=num+1;
			
			if(num==failnum)
			gap=mingaptime+1;
		}
		
		print("iconnect; "+iconnect+"__"+gap/1000+" sec"+"   "+input);
		wait(2500);
		
		if(gap>mingaptime+mingaptime*0.25)
		success=success+1;
	}
	
	print("success; "+success+" times");
	
	pathindex=lastIndexOf(input,"/");
	parentsdir=substring(input,0,pathindex);
	
	origidir=substring(input,0,pathindex+1);
	
	dotindex= lastIndexOf(input,".");
	filename=substring(input,pathindex+1,dotindex);
	
	pathindex=lastIndexOf(parentsdir,"/");
	parentsdir=substring(parentsdir,0,pathindex+1);
	
	print("parentsdir; "+parentsdir);
	
	if(success==round(connectiontime)){
		
		saveAs("ZIP", input);
	}else{
		saveAs("ZIP", parentsdir+filename+"_suc_"+success+"_fail");
		File.saveString("fail", origidir+filename+"_fail.txt"); //- Saves string as a file. 
	}
	run("Close All");
}


run("Misc...", "divide=Infinity save");

run("Quit");



