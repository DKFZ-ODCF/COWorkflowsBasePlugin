import sys,os,string

import xlrd, xlwt


class Filter:
	
	def __init__(self,function,rowNo=None,colNo=None):
		self.function = function
		self.rowNo = rowNo
		self.colNo = colNo
		self.passed = 0
		self.filtered = 0
	
	def testLine(self,sheet,rowNo):
		cell = str(sheet.cell_value(rowNo,self.colNo))
		return self.function(cell)



def moreThenOneCaller(st):
	"""Returns true if more than one caller has identified the SNP
	<st> is a string that contatins the names of caller seperated by comma, e.g. varscan,samtools,snv-mix"""
	cols = string.split(st,',')
	if len(cols) > 1:
		return True
	else:
		return False


### Read Excell workbooks ###

def workbookInfo(fp):
	book = xlrd.open_workbook(fp)
	print "The number of worksheets is", book.nsheets
	print "Worksheet name(s):", book.sheet_names()
	sh = book.sheet_by_index(0)
	print sh.name, sh.nrows, sh.ncols
	print "Cell D30 is", sh.cell_value(rowx=0, colx=0)
	print "Type: ",type(sh.cell_value(rowx=0, colx=0))
	#for rx in range(sh.nrows):
	#	print sh.row(rx)


def parseWorksheetToDictionary(sheet,refCol,skipHeader=0,processLine=None):
	"""Parses the whole <sheeet> and transforms rows in tab-seperated lines that are stored in dic by <refCol> column as key """
	dic = {}
		
	for rowNo in range(skipHeader,sheet.nrows):
		line = str(sheet.cell_value(rowx=rowNo, colx=0))
		for colNo in range(1,sheet.ncols):
			line += "\t%s"%str(sheet.cell_value(rowx=rowNo, colx=colNo))
		line += "\n"
		if processLine:
			line = processLine(line)
		key = refCol
		if refCol!=None:
			key = str(sheet.cell_value(rowx=rowNo, colx=refCol))
		try:
			dic[key].append(line)
		except KeyError:
			dic[key] = [line]
	return dic

def dicToFiles(dic,outbasename,outpath):
	"""Distribute lines in dic to files"""
	if len(dic.keys()) == 1 and dic.has_key(None):
		out = open(os.path.join(outpath,"%s.txt"%(outbasename)),'w')
		for line in dic[None]:
			out.write(line)
		out.close()
	else: 
	    for key in dic.keys():
		out = open(os.path.join(outpath,"%s_%s.txt"%(outbasename,key)),'w')
		for line in dic[key]:
			out.write(line)
		out.close()
	

def convertWorkbookToTxtFilesSplitSheets(fp,refCol,outbasename,outpath,skipHeader=0,filterObject=None,processLine=None):
	"""Converts a xls file into a number of txt files, with file corresponding to one worksheet"""
	book = xlrd.open_workbook(fp)
	sheetNames = book.sheet_names()
	sheetCount = book.nsheets
	
	for i in range(0,sheetCount):
		sheet = book.sheet_by_index(i)
		dic = parseWorksheetToDictionary(sheet,refCol,skipHeader,processLine)
		dicToFiles(dic,"%s_%s"%(outbasename,str(sheet.name)),outpath)

def convertWorksheetToTxtFile(sheet,fpOut,filterObject=None):
	out = open(fpOut,'w')
	for rowNo in range(0,sheet.nrows):
		# Test if filter passes this line
		if filterObject and filterObject.testLine(sheet,rowNo):
			filterObject.passed += 1
		else:
			filterObject.filtered +=1
			continue
		
		# Formats and outputs the line
		line = str(sheet.cell_value(rowx=rowNo, colx=0))
		for colNo in range(1,sheet.ncols):
			line += "\t%s"%str(sheet.cell_value(rowx=rowNo, colx=colNo))
		out.write("%s\n"%line)
	out.close()



def convertWorkbookToTxtFiles(fp,outbasename,outpath,filterObject=None):
	"""Converts a xls file into a number of txt files, with file corresponding to one worksheet"""
	book = xlrd.open_workbook(fp)
	sheetNames = book.sheet_names()
	sheetCount = book.nsheets
	
	for i in range(0,sheetCount):
		sheet = book.sheet_by_index(i)
		fnOut = "%s_%s"%(outbasename,str(sheet.name))
		fpOut = os.path.join(outpath,fnOut)
		convertWorksheetToTxtFile(sheet,fpOut,filterObject)
	


### Write Excel Workbooks ###

def convertTxtFileToWorksheet(fp,sheet):
	f = open(fp)
	rowNo = 0
	for line in f:
		cols = string.split(line[:-1],'\t')
		# treat windows and mac newlines
		#if cols[-1] and cols[-1][-1] == '\n' or cols[-1][-1] == '\r':
		#	cols[-1] = cols[-1][:-1]
		for colNo in range(0,len(cols)):
			sheet.write(rowNo,colNo,cols[colNo])
		rowNo +=1

def old_convertTxtFilesToWorkbook(fns,inpath,inbasename,fileend,fpOut,summary=False):
	"""Convert a list of files <fps> into a workbook
	<fns> a list of filenames
	<inpath> path to directory that contatins txt files
	<inbasename> part of each filename that is substracte before worksheet is generated
	<fileend> part of filename end that is substracted before worksheet is generated
	<fpOut> filepath of the generated outputfile
	<summary> a summary sheet contatining all files is added"""
	style0 = xlwt.easyxf('font: name Times New Roman, color-index red, bold on',
	num_format_str='#,##0.00')
	book = xlwt.Workbook()
	
	# intermediate Solution
	if summary:
		summary  = os.path.join(inpath,"Summary.txt")
		open(summary,'w')
	
	for fn in fns:
		if fn[:len(inbasename)] != inbasename : 
			print fn[:len(inbasename)], inbasename,
			return False
		if fileend and fn[-len(fileend):] != fileend:
			print  fn[-len(fileend):], fileend
			return False
		sheetName = fn[len(inbasename):-len(fileend)]
		if not fileend:
			sheetName = fn[len(inbasename):]
		sheet = book.add_sheet(sheetName)
		convertTxtFileToWorksheet(os.path.join(inpath,fn),sheet)
		if summary:
			# append to summary
			out = open(summary,'a')
			for line in open(os.path.join(inpath,fn)):
				out.write("%s\t%s"%(fn,line))
			out.close()
	if summary:
		sheet = book.add_sheet("Summary")
		convertTxtFileToWorksheet(summary,sheet)
			
	book.save(fpOut)
	
		

def convertTxtFilesToWorkbook(fpDic,fpOut,summary=False):
	"""Convert a list of files <fps> into a workbook
	<fpDic> a dicionary that maps file descriptions to filepaths
	<fpOut> filepath of the generated outputfile
	<summary> if True a summary sheet contatining all files is added"""
	style0 = xlwt.easyxf('font: name Times New Roman, color-index red, bold on',
	num_format_str='#,##0.00')
	book = xlwt.Workbook()
	
	# intermediate Solution
	if summary:
		summary  = os.path.join("Summary.txt")
		open(summary,'w')
	names = fpDic.keys()
	names.sort()
	for sheetName in names:
		sheet = book.add_sheet(sheetName)
		convertTxtFileToWorksheet(fpDic[sheetName],sheet)
		if summary:
			# append to summary
			out = open(summary,'a')
			for line in open(fpDic[sheetName]):
				out.write("%s\t%s"%(sheetName,line))
			out.close()
	if summary:
		sheet = book.add_sheet("Summary")
		convertTxtFileToWorksheet(summary,sheet)
		os.remove(summary)
			
	book.save(fpOut)

if __name__=="__main__":
    from optparse import OptionParser
    parser = OptionParser()
    parser.add_option('-f','--filename',action='store',type='str',dest='fp',help='Inputfile as tab-seperated file \n',default='Test.xls')
    parser.add_option('--filenames',action='store',type='str',dest='fps',help='Inputfiles seperated by ","; e.g. "Name1:File1.txt,Name2:File2.txt"\n',default='')
    #parser.add_option('-s','--column',action='store',type='str',dest='colNo',help='Number of column that should be used to seperate data into worksheets.\n',default='None')
    parser.add_option('-o','--outfile',action='store',type='str',dest='outFp',help='Name of the outputfile.\n',default=None)
    #parser.add_option('-h','--header',action='store',type='str',dest='header',help='If file has a header that schould be ignored.\n',default=None)
    (options, args) = parser.parse_args()
    
    
    dic = {}
    if options.fps != '':
      fs = options.fps.split(',')
      for ft in fs:
	fn = ft.split(':')
	dic[fn[0]] = fn[1]
    else:
      dic["Data"] = options.fp
    convertTxtFilesToWorkbook(dic,options.outFp)
