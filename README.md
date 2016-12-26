# Taxi Tracker - User App

## Projeto

(TODO)

## Taxi Tracker - Library

Os apps para Drivers e Users (hopefully) compartilharão uma grande code base. Para refletir isso, optou-se pela criação de uma Android Library, que será responsável por prover, por exemplo, uma interface de comunicação com a API REST ((TaxiTracker - Backend)[https://github.com/falcaopetri/taxi-tracker-backend]). 

Após algumas leituras de recomendações sobre como estruturar um projeto como esse (shared code base), optou-se por criar tal Android Library como um projeto separado. Assim, essa biblioteca acabou gerando também um novo repositório de versionamento. Essa é uma desvantagem dessa escolha: não é possível associar uma versão da Library com uma versão do App, isto é, elas devem sempre caminhar juntas (ou pelo menos ter backward compatibility durante todo o ciclo de desenvolvimento).

Essa biblioteca foi adicionada como um Module Dependency de cada um dos apps, seguindos instruções [daqui](http://stackoverflow.com/a/22747005) (seguindo o **ALTERNATIVE METHOD** proposto). Isso implica que uma estrutura deve ser seguida.

Suponha que esse projeto está no diretório `$DIR/TaxiTracker-User`. A Taxi Tracker - Library deve **obrigatoriamente** estar em `$DIR/TaxiTracker-Library`.

Note também que ela é um repositório totalmente independente: pushs e pulls devem ser executados tanto para esse App quando para a Library.
