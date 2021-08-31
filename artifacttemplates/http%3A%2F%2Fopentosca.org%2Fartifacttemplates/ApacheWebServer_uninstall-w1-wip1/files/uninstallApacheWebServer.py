#!/usr/bin/env python
import sys
import os

def parseParameterFromArgV(argv, parameter):
    result = None
    for arg in argv:
        if str(parameter) + '=' in arg:
            result = arg[arg.index(str(parameter) + "=") + len(parameter)+1:]
    return result

def printOutput(dict):
    for key in dict:
        print(str(key) + '=' + str(dict[key]))

def main(argv):

    os.system('apt-get remove -yq apache2')
    print('Uninstalled Apache')

if __name__ == "__main__":
   main(sys.argv[1:])
