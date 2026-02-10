# Clusterle

## Description
Wordle is a word game where the player has to guess a mystery word. The mystery word is predefined by the number of letters and how many there are, as well as how many guesses the player has to guess it. After each player guess, the game shows which letters coincide with the mystery word. If the letter is in the correct spot, it will turn green. If the a letter exists in the mystery word but it is in the wrong spot, it will turn yellow. Otherwise, it will be greyed out. The game ends when the player guesses the word or they run out of guesses.

## How to play
On IntelliJ:
  - Clone repo
  - Set Project SDK = JDK 25
  - Run configuration:
    - Main class: Clusterle
    - Working directory: project root (the folder that contains resources, lib, etc.)
    - VM options (Copy exactly as written below):
    
          --module-path lib --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.media,javafx.web,javafx.swing
          -Djava.library.path=javafx-bin
          -Djna.library.path=win32-x86-64
      

At this point in developmet, unless IntelliJ is using Gradle or Maven, only Windows users are able to play the game as the JavaFX bin folder only includes the files for Windows.

## Usage
Self-made wordle is based on the popular online game by New York Times called "Wordle". In contrast to "Wordle" there are three other game modes Self-made Wordle includes, them being Verticle, Xordle and Nerdle. Links for the original games are all found below.
- Wordle: https://www.nytimes.com/games/wordle/index.html
- Verticle: https://verticle.netlify.app/
- Xordle: https://xordle.org/
- Nerdle: https://nerdlegame.com/

## Support
On any questions or suggestions, I am avaiable per mail:
tomas.ferreira@students.fhnw.ch

## Roadmap
- Make it possible for Mac and Linux users to be able to play isn't yet possible. It will be possible in the fututre.
- At the moment, the German word list only includes words with four and five letters. In the future, it will be possible to have six and seven letter words. 
- A possible idea for the future is to make Self-made wordle a multiplayer game, meaning being able to play either with or against other players on two different devices.

## Authors and acknowledgment
### Game modes: 
The game modes used on my programm and researched about them were based on already existing online games from either New York Times or stand-alone websites. All the links to the original games are listed below:
- Wordle: https://www.nytimes.com/games/wordle/index.html
- Verticle: https://verticle.netlify.app/
- Xordle: https://xordle.org/
- Nerdle: https://nerdlegame.com/

### Internet sources

#### Stack Overflow
- The class used to create the background colors seen on the terminal during gameplay was found in this discussion: https://stackoverflow.com/questions/5762491/how-to-print-color-in-console-using-system-out-println/5762502#5762502
- Many occasions in my code use Math.random. To better understand how to use it, the following discussion was read: https://stackoverflow.com/questions/363681/how-do-i-generate-random-integers-within-a-specific-range-in-java

#### W3Schools
- Learned about multidimensional arrays in this platform: https://www.w3schools.com/java/java_arrays_multi.asp

#### Geeksforseeks
- Further research about multidimensional arrays here: https://www.geeksforgeeks.org/java/multidimensional-arrays-in-java/

#### Checkstyle
- Checkstyle plug-in was used to improve code structure. To better understand which problems there were, this website was used: https://checkstyle.org/config.html#Checker

#### Wordlist
- For the English wordlist, this website was used: https://wordraiders.com/wordle-words/
- For the German wordlist, this website was used: https://www.wort-suchen.de/woerterbuch/
- For the dictionairies, Chromium was used: https://chromium.googlesource.com/chromium/deps/hunspell/%2B/9f292621c01a6708cd35416c4482ba5d6881186f/dictionaries?
- For the libraries, this repo was used: https://github.com/LibreOffice/dictionaries

### Opensource AI
For guiding methods and correction purposes, Chatgpt was used.

### Peer and professor interaction
Through exchange of ideas and knowledge, more diverse aspects and functions could be applied on this project

## License
This project uses the MIT license
