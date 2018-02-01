# goose - entity search using generative algorithms
The documents will be created at search time. Pre document generation is not available.
No spaces in paths allowed.

## Download DBpedia files
use script https://github.com/dice-group/NLIWOD/blob/master/qa.hawk/deploy-scripts/index_fuseki.sh until line 90 to donwload all neccessary DBpedia files

## Create triple Store
use knowledgeBase.TDBCreator#main with 
- 1st argument: path where tdb will be created 
- 2nd argument: path to DBpedia files 

to create tdb 

example args: /home/goose/tdb /home/goose/dbpedia
## Create index for OTF-Mode
use knowledgeBase.DocumentGenerator#main with
- 1st argument: path to tdb
- 2nd argument: path where index will be stored in

to create the index for the OTF-Mode

example args: /home/goose/tdb /home/goose/index

## Direct search
use search.DocumentSearcher#main with
- 1st argument: path to index
- 2nd argument: path where the temporary index will be created
- 3rd argument: path to tdb
- 4th argument: 0 for Take only S P O, 1 for Take considering Pagerank 
- other arguments: keywords of questions seperated by ,

to search directly

example args: /home/goose/index /home/goose/otfindex /home/goose/tdb 0 Bill Gates, wife, birthplace

## Evaluation
use evaluation.OTFEvaluation#main with
- 1st argument: path to index
- 2nd argument: path where the temporary index will be created
- 3rd argument: path to tdb
- 4th argument: 0 for Take only S P O, 1 for Take considering Pagerank 
- 5th argument: path where the evaluation file will be created

to run evaluation 

example args: /home/goose/index /home/goose/otfindex /home/goose/tdb 0 /home/goose/evaluation.txt
