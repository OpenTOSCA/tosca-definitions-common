#!/usr/bin/env python
import sys
import os
import subprocess

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
    port = parseParameterFromArgV(argv, "Port")
    rootpath = parseParameterFromArgV(argv, "Rootpath")

    os.system('export DEBIAN_FRONTEND=noninteractive && apt-get install -yq apache2')

    output = subprocess.run(['apache2','-v'],check=True, stdout=subprocess.PIPE, universal_newlines=True)
    for line in str(output.stdout).split('\n'):
        if 'Server version:' in line:
            version = line[len('Server version:'):].strip()

    os.system('service apache2 stop')

    # Share Output via printing as the last lines
    printOutput({'Port': port, 'Rootpath' : rootpath, 'Version' : version})

if __name__ == "__main__":
   main(sys.argv[1:])
