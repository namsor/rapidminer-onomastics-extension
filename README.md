RapidMiner Onomastics Extension
===============================

NamSor Applied Onomastics extension for RapidMiner, includes the following operators:
- Extract Gender

# Introduction

If you are reading this tutorial, you probably have already installed RapidMiner and gained some experience by playing around with the enormous set of operators.
At NamSor, we intend to deliver a set of operators for mining proper names in all geographies/alphabets/languages/cultures. We decided to start with a seemingly simple operator GendRE Gengerizer, to predict the likely gender of a personal name. Other useful operators will come in 2014. 
Guessing the gender of name is not as simple as it seems:
- Andrea is a male name in Italy, a female name in the US. Laurence is a female name in France and a male name in the UK or in the US
- name demographics evolve, some names are genderless
- in Chinese or Korean, guessing the gender is almost impossible in Latin script, truly difficult even with the original script
- in most cultures, the gender is 'encoded' in the first name, in others it is encoded in the last name as well (for example, Slavic names, Lithuanian names ...) so you can guess the gender even if you have just the initials (for example, O. Sokolova is most likely a Slavic name and a female name)
- some names are very rare or just 'made up' and yet, because they sound like a male name or a female name, their gender is accurately perceived by the people in that same culture
GendRE Genderizer API goal is to hide this complexity, offer a simple interface and return an optimal result:
api/json/gendre/John/Smith
{"scale":-0.99,"gender":"male"}

Can you guess the result of the following?
api/json/gendre/בנימין/נתניהו/il 
api/json/gendre/声涛/周
api/json/gendre/معين/المرعبي/lb
Currently, we require input names to be properly parsed into a (first_name, last_name) format and our machine learning algorithm will progressively discover how names are parsed in different cultures. When this calibration is complete, we'll offer an even simpler interface. 
In RapidMiner, simply connect the Extract Gender operator in your process to infer the gender of a personal name and create new data/new segmentation.

# Getting Started
This section will get you started with NamSor Onomastics Extension. 

## Installation

Pre-compiled extension binaries can be found in GitHub /dist/ directory.

Use RapidMiner MarketPlace:
// add snapshot

Install manually in RapidMiner 6:
Copy the Extension binary into RapidMiner extension directory. For example, on Windows:
C:\Program Files\RapidMiner\RapidMiner Studio\lib\plugins\rapidminer-NamSor-6.0.001.jar

Install manually in RapidMiner 5.3:
Copy the Extension binary into RapidMiner extension directory. For example, on Windows:
C:\Program Files (x86)\Rapid-I\RapidMiner5\lib\plugins\rapidminer-NamSor-5.3.001.jar

## Create your first process
Create a simple Excel document with columns first_name, last_name and a few contacts.
Drag and drop the Read Excel operator (Import->Data->Read Excel) and launch the Import Configuration Wizard.
Default values should be OK through the wizard, except Encoding should be set to UTF-8 (Unicode, especially required if you would like to genderize Chinese, Russian or Arabic names).

Drag and drop the Extract Gender operator (Operators>Data Transformation/Attribute Set Reduction and Transformation/Generation> Extract Gender) and connect it with the Excel file and a CSV exporter to view the results. 
 
Leave the api_key / api_channels to default to use the free GendRE API.

## Network/API troubleshooting
In case of network error,
- check that you can access the API from behind your proxy, using your ordinary browser
http://api.onomatic.com/onomastics/api/json/gendre/John/Smith
{"scale":-0.99,"gender":"male"}

- check your RapidMiner proxy configuration in Tools>Preferences>System

## Getting support
Please report any issue with the software or the GendRE API via GitHub
https://github.com/namsor/rapidminer-onomastics-extension/issues

You can upgrade the GendRE API (better precision) and subscribe for commercial support.

## Upgrading GendRE API on Mashape.com
You can upgrade the GendRE API (for better precision) and subscribe for commercial support.
- Register on Mashape.com to obtain an API Key
https://www.mashape.com/namsor/gendre-infer-gender-from-world-names

In the Operator parameters,
- api_key: enter your Mashape.com API Key
- api_channel: enter mashape.com
 
To report an issue or ask a question to our support team, please use
https://www.mashape.com/namsor/gendre-infer-gender-from-world-names#!issues

# To contact us
contact@namsor.com
