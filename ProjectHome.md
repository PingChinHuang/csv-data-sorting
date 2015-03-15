This is a command line tool to sort a specified column in a CSV file. The data column in a CSV file is seperated with comma by default. In fact, you can assign the seperator you want, but I did not implemented the command line option for configuring it. Merge sort is used to complete the sorting procedure.

I implemented this program based on JavaSE-1.6.
The usage of this program shows as follows:
Usage
> java SortCSVData [-verbose]  [-sorting [or \"type\"](type.md) [-output file]  filename.csv

Options
> -verbose
> > Verbose the options you give to this program.


> -sorting type or "type"
> > The data type you want to sort in the .csv file. If the name of data type is a combination of two or more strings, please use double quote to quote the strings. The data type you specified must be in the .csv file or an error will be raised.


> -output file
> > Specify the file name which is used to write the sorted CSV table into.


> filename.csv
> > The .csv file you want to sort. The extension name of file should be csv, and it is case-insensitive.


> Other
> > The column sorting will be done automatically after sorting specified data type.