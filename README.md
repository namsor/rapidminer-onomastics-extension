RapidMiner Onomastics Extension
===============================

NamSor Applied Onomastics extension for RapidMiner, includes the following operators to infer valuable information from personal names:
- Parse Name
- Extract Gender
- Extract Origin

Read the full documentation (PDF) https://github.com/namsor/rapidminer-onomastics-extension/blob/master/doc/201511_NamSor_RapidMiner_Extension_v007.pdf?raw=true

# Why should I register on NamSor?
If you set an API Key in the Extract Gender operator, you'll have
- higher performance & throughput (hundreds of names processed at a time)
- full double precision
- commercial support 

Registration is required for Extract Origin operator.

Get a Freemium API Key for Extract Gender and Extract Origin
https://api.namsor.com/

# About NamSor
NamSor™  is a European vendor of Name Recognition Software. We offer specialized data mining to recognize the origin of personal names in any alphabet / language, with fine grain and high accuracy. 

NamSor's mission is to help understand international flows of money, ideas and people. 
Our values: we promote diversity, equal opportunity and support the @GenderGapGrader initiative.
http://www.namsor.com/

# Introduction

If you are reading this tutorial, you probably have already installed RapidMiner and gained some experience by playing around with the enormous set of operators.
At NamSor, we intend to deliver a set of operators for mining proper names in all geographies/alphabets/languages/cultures. 

Click to view the demo video on Youtube:
[![NamSor RapidMider Extension](http://img.youtube.com/vi/VWSZBxXd4OY/0.jpg)](http://www.youtube.com/watch?v=VWSZBxXd4OY "NamSor RapidMider Extension")

# Extract Gender Operator
NamSor Gender (formerly GendRE) predicts the likely gender of a personal name. Guessing the gender of name is not as simple as it seems:
- Andrea is a male name in Italy, a female name in the US. Laurence is a female name in France and a male name in the UK or in the US
- name demographics evolve, some names are genderless
- in Chinese or Korean, guessing the gender is almost impossible in Latin script, truly difficult even with the original script
- in most cultures, the gender is 'encoded' in the first name, in others it is encoded in the last name as well (for example, Slavic names, Lithuanian names ...) so you can guess the gender even if you have just the initials (for example, O. Sokolova is most likely a Slavic name and a female name)
- some names are very rare or just 'made up' and yet, because they sound like a male name or a female name, their gender is accurately perceived by the people in that same culture
NamSor Gender API goal is to hide this complexity, offer a simple interface and return an optimal result:
api/json/gender/John/Smith
{"scale":-0.99,"gender":"male"}

Can you guess the result of the following?
api/json/gender/בנימין/נתניהו/il 
api/json/gender/声涛/周
api/json/gender/معين/المرعبي/lb
Currently, we require input names to be properly parsed into a (firstName, lastName) format and our machine learning algorithm will progressively discover how names are parsed in different cultures. When this calibration is complete, we'll offer an even simpler interface. 
In RapidMiner, simply connect the Extract Gender operator in your process to infer the gender of a personal name and create new data/new segmentation.

# Extract Origin Operator
NamSor Origin will guess the likely country of origin of a personal name, based on the sociolinguistics of the name (language, culture). This is a coarse grain classification, typically for marketing or social analytics. Finer-grain classification (regional level, ethnicity...) is available for more complex usage like Diversity Analytics or Migration Studies, but requires a specific paper contract.

The method for anthroponomical classification can be summarized as follow: judging from the name only and the publicly available list of all ~150k Olympic athletes since 1896 (and other similar lists of names), for which national team would the person most likely run? Here, the United-States, Australia, etc. are typically considered as a melting pot of other ‘cultural origins’: Ireland, Germany, etc. and not as a onomastic class on its own.

# NamSor API is also on Mashape

Get a Freemium API Key for Extract Gender
https://market.mashape.com/namsor/gendre-infer-gender-from-world-names

Get a Freemium API Key for Extract Origin
https://market.mashape.com/namsor/origin

# To contact us
contact@namsor.com
