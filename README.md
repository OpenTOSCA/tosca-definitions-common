# TOSCA Definitions Common Repository [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

> Common TOSCA definitions (e.g., VMs, cloud providers, and runtimes) prepared for being consumed by a TOSCA-compliant engine, such as the [OpenTOSCA Container](http://opentosca.github.io/container).

## Usage

This repository uses [git lfs](https://git-lfs.github.com/) as it contains large binary files which are not checked in to the git tree.
Therefore, make sure that
  1. you have an active git lfs installation on your machine,
  2. run `git lfs install` directly after cloning the repository inside the cloned folder, and
  3. run `git lfs pull` to download the binary files.

⚠️ If the lfs-files are not downloaded, the repository, and thus the modeled applications, cannot run! 
Thus, ensure you run `git lfs pull`.

💡 To install git lfs globally, run `git lfs install --system`. 
This makes git lfs automatically available for all repositories, and you do not have to run git lfs pull yourself.

## Haftungsausschluss

Dies ist ein Forschungsprototyp und enthält insbesondere Beiträge von Studenten.
Diese Software enthält möglicherweise Fehler und funktioniert möglicherweise, insbesondere bei variierten oder neuen Anwendungsfällen, nicht richtig.
Insbesondere beim Produktiveinsatz muss 1. die Funktionsfähigkeit geprüft und 2. die Einhaltung sämtlicher Lizenzen geprüft werden.
Die Haftung für entgangenen Gewinn, Produktionsausfall, Betriebsunterbrechung, entgangene Nutzungen, Verlust von Daten und Informationen, Finanzierungsaufwendungen sowie sonstige Vermögens- und Folgeschäden ist, außer in Fällen von grober Fahrlässigkeit, Vorsatz und Personenschäden ausgeschlossen.

## Disclaimer of Warranty

Unless required by applicable law or agreed to in writing, Licensor provides the Work (and each Contributor
provides its Contributions) on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
or implied, including, without limitation, any warranties or conditions of TITLE, NON-INFRINGEMENT,
MERCHANTABILITY, or FITNESS FOR A PARTICULAR PURPOSE. You are solely responsible for determining the
appropriateness of using or redistributing the Work and assume any risks associated with Your exercise of
permissions under this License.
