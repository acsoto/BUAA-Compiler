

24.7.30 Actualizo este README. He descubierto que muchos compañeros de cursos inferiores han consultado este repositorio, e incluso hace dos años un compañero vino varias veces a preguntar con detalle sobre muchas implementaciones e ideas dentro del código. Pensé que a la gente no le interesaba escribir PCode, pero no esperaba poder ayudar a todos. Estoy muy agradecido por el reconocimiento. Me preguntaron cosas que yo mismo no recordaba, por lo que tuve que releer el código y la documentación, lo que me sirvió para repasar todo de nuevo por completo.

Anteriormente había muchos errores gramaticales en el inglés, así que los he corregido aproximadamente. Además, la estructura algo confusa de "Before Coding / After Coding" se debe a que el curso exigía documentar las ideas de diseño antes y después de programar, por lo que he mantenido este formato.

---

Puedes cambiar de rama para ver el código de cada fase de diseño, dividido en cuatro partes: análisis léxico, análisis sintáctico, manejo de errores y generación de código.

Se diseñó un autómata finito para el análisis léxico, se procesan los caracteres ilegales, se construye un árbol sintáctico abstracto, se utiliza el método de descenso recursivo para el análisis sintáctico y el manejo de errores, se compila el lenguaje SysY a código PCode y, finalmente, se diseñó una máquina virtual correspondiente para su ejecución interpretada.


## Análisis Léxico Lexical Analysis

### Antes de la Programación (Before Coding)

Requisito: Leer `testfile.txt`, analizar cada carácter en palabras e imprimirlas. Al mismo tiempo, memorizar el tipo, el contenido y el número de línea de cada palabra.

#### Lectura de archivos

Leer línea por línea, escanear cada carácter de cada cadena y analizar.

```java
while ((s = bf.readLine()) != null) {
	...
}
```

#### Análisis

Después de identificar el token actual, continuar con el siguiente paso del análisis.

```java
while ((c = getChar()) != null) {
  if (c == ' ' || c == '\r' || c == '\t') {
    continue;
  } else if (c == '+' || c == '-' || c == '*' || c == '%') {
    words.add(new Word(c));
  } else if (c == '/') {
    analyseSlash();
  } else if (c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}') {
    words.add(new Word(c));
  } else if (c == '>' || c == '<' || c == '=' || c == '!') {
    analyseRelation(c);
  } else if (c == ',' || c == ';') {
    words.add(new Word(c));
  } else if (c == '"') {
    analyseCitation();
  } else if (c == '&' || c == '|') {
    analyseLogic(c);
  } else if (Character.isDigit(c)) {
    analyseDigit(c);
  } else if (Character.isLetter(c) || c == '_') {
    analyseLetter(c);
  }
}
```

##### Comunes

Por ejemplo, al encontrar `+`, crear directamente un nuevo `Word` y clasificarlo como `PLUS`.

##### Funciones

Por ejemplo

Al encontrar `<`, ingresar a la función `analyseRelation` para leer un carácter más. Si es `=`, clasificarlo como `LEQ`...

```java
if (c == '<') {
  c = getChar();
  if (c == '=') {
    words.add(new Word("<="));
  } else {
    unGetChar();
    words.add(new Word("<"));
  }
```

`analyseLogic` funciona de la misma manera.

##### Dígitos y Letras

Dígito: Al encontrar un dígito, escanear una secuencia de dígitos y convertirla en un `Word` clasificado como `INTCON`.

Letra: Al encontrar una letra, escanear una cadena de letras o dígitos. Puede convertirse en `IDENFR` o `STRCON`, dependiendo de si existe en el mapa de palabras clave.

#### Palabra

Clase Word:

```java
public class Word {
    private String identification;
    private String content;
    private String type;
}
```

Se encapsula la lógica de inicialización para que solo sea necesario `new Word(...)` en el procesador principal, lo cual creará el token correspondiente.

Por ejemplo

```java
    public Word(char identification) {
        this.identification = String.valueOf(identification);
        this.type = new KeyWordMap().getType(this.identification);
        this.content = this.identification;
    }
```

`KeyWordMap` es un `HashMap` que asigna cada cadena de token a su tipo.

```java
    public KeyWordMap() {
        keyWords = new HashMap<>();
        keyWords.put("main", "MAINTK");
        keyWords.put("const", "CONSTTK");
        keyWords.put("int", "INTTK");
        ...
        }
```

### Después de la Programación (After Coding)

#### Lectura de archivos


Leer el archivo línea por línea no es conveniente para operaciones de visión anticipada (lookahead) o deshacer, por lo que el archivo se lee primero en una única `String`.

El método lee el archivo línea por línea, anexa `\n` después de cada línea y luego escanea cada carácter. Cuando se encuentra `\n`, `lineNum++`.

```java
    private String transferFileToCode() {
        BufferedReader bf = new BufferedReader(reader);
        StringBuffer buffer = new StringBuffer();
        String s = null;
        while ((s = bf.readLine()) != null) {
            buffer.append(s).append("\n");
        }
        return buffer.toString();
    }
```

#### Análisis

En cuanto al análisis, es diferente a lo descrito antes de la programación.

Primero, los tokens deben analizarse uno por uno, por lo que se añade una variable global `index` para rastrear la posición actual del puntero.

Además, pueden surgir situaciones donde sea necesario leer un carácter más o deshacer una lectura, por lo que las funciones `ungetChar` y `getChar` están encapsuladas para facilitar el análisis.

```java
    private Character getChar() {
        if (index < code.length()) {
            char c = code.charAt(index);
            if (c == '\n') {
                lineNum++;
            }
            index++;
            return c;
        } else {
            return null;
        }
    }

    private void unGetChar() {
        index--;
        char c = code.charAt(index);
        if (c == '\n') {
            lineNum--;
        }
    }
```

##### Barra (/)

1) `//` : Al llegar a `\n`, detenerse.

```java
do {
  c = getChar();
  if (c == null || c == '\n') {
    return;
    // 判断为//注释，结束分析
  }
} while (true);
```

2) `/* */`: Leer caracteres hasta que aparezca `*/`.

```java
do {
  c = getChar();
  if (c == null) {
    return;
  }
  if (c == '*') {
    c = getChar();
    if (c == '/') {
      return;
      // 判断为/* */注释，直接结束分析
    } else {
      unGetChar();
    }
  }
} while (true);
```

## Análisis Sintáctico Grammar Analysis 

Requisito: Basado en las palabras identificadas por el programa de análisis léxico, identificar varios elementos gramaticales según las reglas gramaticales. Se utiliza el método de descenso recursivo para analizar los componentes gramaticales definidos en la gramática.

### Antes de la Programación (Before Coding)

#### Lectura de datos

Similar al análisis léxico, se preparan funciones como `getWord` y `getNextWord`. Además, existe una variable global `(Word) curWord` para indicar la palabra actual al leer `ArrayList<Word> words` del análisis léxico uno por uno.

La estrategia de análisis es la siguiente:

- Para reglas normales: Seguir obteniendo palabras y analizarlas.
- Para reglas de expresión: Primero, escanear toda la expresión usando la función `getExp`. Luego, dividir la expresión y usar el método de descenso recursivo para analizarla.


`getExp` como

```java
    private ArrayList<Word> getExp() {
        ArrayList<Word> exp = new ArrayList<>();
        while (true) {
            if (word is symbol of end) {
                break;
            }
            ...
            getWordWithoutAddToGrammar();
            exp.add(curWord);
            word = getNextWord();
        }
        return exp;
    }
```

#### descenso recursivo

De acuerdo con las reglas gramaticales, codificar funciones para cada término de la regla.

Idea principal: Leer una palabra, verificar qué simboliza e ingresar a la siguiente función de análisis.

Por ejemplo:

a

```c
CompUnit → {Decl} {FuncDef} MainFuncDef // 1.是否存在Decl 2.是否存在 FuncDef
```

Lo analizo así:

```java
private void analyseCompUnit() {
  Word word = getNextWord();
  while (word.typeEquals("CONSTTK") || (
    word.typeEquals("INTTK") && getNext2Word().typeEquals("IDENFR") && !getNext3Word().typeEquals("LPARENT"))) {
    analyseDecl();
    word = getNextWord();
  }
  while (word.typeEquals("VOIDTK") || (
    (word.typeEquals("INTTK") && !getNext2Word().typeEquals("MAINTK")))) {
    analyseFuncDef();
    word = getNextWord();
  }
  if (word.typeEquals("INTTK") && getNext2Word().typeEquals("MAINTK")) {
    analyseMainFuncDef();
  } else {
    error();
  }
  grammar.add("<CompUnit>");
}
```

`grammar` se utiliza para almacenar la salida de tanto las listas de análisis léxico como de análisis sintáctico.

#### recursión a la izquierda

```java
加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp // 1.MulExp 2.+ 需覆盖 3.- 需覆盖
```

Verifica si la expresión contiene '+' o '-'. Si es así, separa la expresión en `AddExp` y `MulExp`. Luego analícelas por separado.

### Después del Código (After Code)

#### recursión a la izquierda

El método anterior no es perfecto para el descenso recursivo. Por lo tanto, el enfoque ha sido revisado y reescrito.

a

```c
加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp // 1.MulExp 2.+ 需覆盖 3.- 需覆盖
```

Reescribirlo como 

```c
AddExp → MulExp ('+' | '−') MulExp  ('+' | '−') MulExp ...
```

Código como 

```java
private void analyseMulExp(ArrayList<Word> exp) {
  Exps exps = divideExp(exp, new ArrayList<>(Arrays.asList("MULT", "DIV", "MOD")));
  int j = 0;
  for (ArrayList<Word> exp1 : exps.getWords()) {
    analyseUnaryExp(exp1);
    grammar.add("<MulExp>");
    if (j < exps.getSymbols().size()) {
      grammar.add(exps.getSymbols().get(j++).toString());
    }
  }
}
```


La función `divideExp` se utiliza para dividir toda la expresión pasada por `getExp` o una función anterior.

`divideExp`:

**Entrada:** 
- Expresión original: `exp`
- Símbolo de parada: `symbol`

**Salida:**
- Lista de expresiones divididas y símbolos.

```java
private Exps divideExp(ArrayList<Word> exp, ArrayList<String> symbol) {
  ArrayList<ArrayList<Word>> exps = new ArrayList<>();
  ArrayList<Word> exp1 = new ArrayList<>();
  ArrayList<Word> symbols = new ArrayList<>();
  boolean unaryFlag = false;
  int flag1 = 0;
  int flag2 = 0;
  for (int i = 0; i < exp.size(); i++) {
    Word word = exp.get(i);
    if (word.typeEquals("LPARENT")) {
      flag1++;
    }
    if (word.typeEquals("RPARENT")) {
      flag1--;
    }
    if (word.typeEquals("LBRACK")) {
      flag2++;
    }
    if (word.typeEquals("RBRACK")) {
      flag2--;
    }
    if (symbol.contains(word.getType()) && flag1 == 0 && flag2 == 0) {
      //UnaryOp
      if (word.typeOfUnary()) {
        if (!unaryFlag) {
          exp1.add(word);
          continue;
        }
      }
      exps.add(exp1);
      symbols.add(word);
      exp1 = new ArrayList<>();
    } else {
      exp1.add(word);
    }
    unaryFlag = word.typeEquals("IDENFR") || word.typeEquals("RPARENT") || word.typeEquals("INTCON") || word.typeEquals("RBRACK");
  }
  exps.add(exp1);
  return new Exps(exps, symbols);
}
```

`Exps`

```java
public class Exps {
    private ArrayList<ArrayList<Word>> words;
    private ArrayList<Word> symbols;
}
```

#### otros errores

La mayoría de los errores se producen en las funciones `getExp` y `divideExp` debido a algunas situaciones pasadas por alto, lo que a menudo resulta en errores como índice fuera de rango. Por lo tanto, se realizaron ajustes en algunos símbolos para detener el análisis de la expresión y se modificaron las reglas para dividir o no dividir la expresión, entre otros cambios.

## Manejo de Errores Error Handling

### Antes de la Programación (Before Coding)

#### Crear la tabla de símbolos

Clase Symbol

```java
public class Symbol {
    private String type;
    private int intType;
    private String content;
    private int area = 0;
}
```

Type representa el tipo del símbolo.

- `IntType` es un entero. Si es 0, el símbolo es un int. Si es 1, el símbolo es un int[]. Si es 2, el símbolo es un int[][], y así sucesivamente.

`Content` es su contenido.

`Area` indica dónde se encuentra.

Se crea un `HashMap` de Símbolos para memorizar los símbolos creados en cada área.

Al entrar en un nuevo área, `area++`. Al salir de un área, `area--`, destruyendo los símbolos correspondientes.

```java
    private HashMap<Integer, Symbols> symbols = new HashMap<>();
    private HashMap<String, Function> functions = new HashMap<>();
    private ArrayList<Error> errors = new ArrayList<>();
    private int area = -1;
    private boolean needReturn = false;
    private int whileFlag = 0;
```

`needReturn` indica si la función actual necesita retornar.

`whileFlag` indica si el bloque de código actual está dentro de un bucle while.

#### Errores

##### **a**

Solo verificar el formato.

```java
public boolean isFormatIllegal() {
  for (int i = 1; i < content.length() - 1; i++) {
    char c = content.charAt(i);
    if (!isLegal(c)) {
      if (c == '%' && content.charAt(i + 1) == 'd') {
        continue;
      }
      return true;
    } else {
      if (c == '\\' && content.charAt(i + 1) != 'n') {
        return true;
      }
    }
  }
  return false;
}
```

##### **b c**

B: Cada vez que se encuentra un identificador, verificar si el mismo símbolo ya ha sido definido en el área actual.

```java
    private boolean hasSymbolInThisArea(Word word) {
        return symbols.get(area).hasSymbol(word);
    } 
```

C: Verificar todas las áreas. Si el símbolo ha sido definido, manejar las funciones de la misma manera.

```java
    private boolean hasSymbol(Word word) {
        for (Symbols s : symbols.values()) {
            if (s.hasSymbol(word)) {
                return true;
            }
        }
        return false;
    }
```

##### **d e**

Para verificar si los parámetros de la función coinciden, se memorizan los parámetros de cada función. Cuando se encuentra una llamada a función, se escanean y coinciden los parámetros de la llamada. Se preparó una función para manejar esto. Se descubrió que nuevamente se necesita descenso recursivo, por lo que el procedimiento de verificación se añadió al descenso recursivo del analizador sintáctico. Consulte `After Code/Error d and e`.

##### **f g**

Existe una variable global `needReturn` utilizada para indicar si la función actual necesita retornar. Si lo hace, pero no hay un retorno al final del bloque de código, o si no lo hace, pero hay un retorno, se registrará el error.

##### **h**

Simplemente verificar si es una constante.

```java
if (isConst(word)) {
  error("h", word.getLineNum());
}
```

##### **i j k**

Encapsular la función para verificar el símbolo faltante.

Por ejemplo:

```java
    private void checkParent() {
        if (getNextWord().typeEquals("RPARENT")) {
            getWord();// )
        } else {
            error("j");
        }
    }
```

##### **l**

Contar el número de parámetros para `string` y `printf` por separado y verificar si son iguales.

##### **m**

Existe una variable global `whileFlag` que indica si el bloque de código está dentro de un bucle while. Si no lo está, cualquier declaración `continue` o `break` producirá un error.

### Después de la Programación (After Coding)

#### Área

Incremento `area++` al entrar en un bloque o una función, pero esto lleva a una situación donde los parámetros de la función no se pueden memorizar en un área diferente del bloque de la función. Por lo tanto, cambié las reglas para marcar `area++`.

```java
    private boolean analyseBlock(boolean fromFunc) {
        ...
        if (!fromFunc) {
            addArea();
        }
        ...
    }
```

Solo cuando el bloque no proviene de la función, se incrementa el área.

#### Error d y e

Para verificar si los parámetros de la función coinciden, se establece un arreglo para cada función.

```java
public class Function {
    private String type;
    private String content;
    private String returnType;
    private ArrayList<Integer> paras;
}
```

Al encontrar una función, se memorizan su tipo de retorno y sus parámetros.

Para `ArrayList<Integer> paras`, se refleja de la siguiente manera:

| Type     | Example | Integer |
| -------- | ------- | ------- |
| Void     |         | -1      |
| Int      | a       | 0       |
| Int[]    | a[]     | 1       |
| Int[] [] | a[] [3] | 2       |

Por lo tanto, al encontrar una llamada a función, los parámetros se verificarán contra lo que se memorizó anteriormente.

```java
private void checkParasMatchRParas(Word ident, ArrayList<Integer> paras, ArrayList<Integer> rparas) {
    if (paras.size() != rparas.size()) {
        error("d", ident.getLineNum());
    } else {
        for (int i = 0; i < paras.size(); i++) {
            if (!paras.get(i).equals(rparas.get(i))) {
                error("e", ident.getLineNum());
            }
        }
    }
}
```

Para obtener el tipo real de los parámetros, el procedimiento de análisis se añade al descenso recursivo del analizador sintáctico. Por ejemplo:

```java
    private int analyseExp(ArrayList<Word> exp) {
        int intType = analyseAddExp(exp);
        grammar.add("<Exp>");
        return intType;
    }
```

Cada recursión devolverá un `intType`, simbolizando el tipo final de la expresión.

Debido a que los términos de una expresión deben ser del mismo tipo, solo se devuelve uno de ellos.

Esta es la salida de la recursión. Devolverá el tipo correcto de la expresión a la parte superior de la función.

```java
    private int analyseLVal(ArrayList<Word> exp) {
        int intType = 0;
        ...
                if (word.typeEquals("LBRACK")) {
                    intType++;
                    ...
                }
         ...
        if (hasSymbol(ident)) {
            return getSymbol(ident).getIntType() - intType;
        } else {
            return 0;
        }
    }
```

## Generación de Código Code Generation 

En esta parte, elegí generar PCode.

Diseñé un tipo de PCode que es un código virtual basado en una pila de expresiones en notación polaca inversa y una tabla de símbolos.

Al mismo tiempo, diseñé una máquina virtual para ejecutarlo.

La máquina virtual de PCode es una máquina imaginaria utilizada para ejecutar comandos PCode. Consiste en un área de código (code), un puntero de instrucciones (EIP), una pila, una var_table, una func_table y una label_table.

En el siguiente apartado, primero introduciré cómo se ejecuta PCode y luego explicaré cómo producir PCode.

### Antes de la Programación (Before Coding)

#### Cómo se ejecuta la máquina virtual

Primero, necesitamos una lista `codes` y una `stack` (int).

Un `eip` representa la dirección del código que se está ejecutando actualmente.

Una `varTable` memoriza la dirección de la variable en la pila.

Una `funcTable` memoriza la dirección de la función en la lista de códigos.

Una `labelTable` memoriza la dirección de la etiqueta en la lista de códigos.

Luego, ejecutar el código uno tras otro y gestionar la pila.

#### Cómo distinguir diferentes variables

Antes de generar el código, diferenciar las variables de diferentes ámbitos por su número de ámbito único, como: `areaID + "_" + curWord.getContent()`. En esta situación, una variable no aparecerá más de una vez en los códigos, excepto en llamadas a funciones recursivas, lo cual se resolverá empujando la `varTable` a la pila (se muestra más adelante).

#### Definición Específica de Código

Primero, definir una clase para PCode:

```java
public class PCode {
    private CodeType type;
    private Object value1 = null;
    private Object value2 = null;
}
```

Representa un objeto de código, que tiene un `CodeType` y dos valores operativos. `CodeType` es un enum. `Value1` y `Value2` pueden ser Integer, String o null, dependiendo del tipo de código específico.

##### Tipo de Cálculo

Dos operadores:

```java
int b = pop();
int a = pop();
push(cal(a,b));
```

Operador único:

```java
push(cal(pop()));
```

##### VAR

El comando **VAR** declara una variable, guardando el nombre de la variable y la dirección asignada en la tabla de variables.

```java
case VAR: {
    Var var = new Var(stack.size());
    varTable.put((String) code.getValue1(), var);
}
```

Var.class:

```java
public class Var {
    private int index;
    private int dimension = 0;
    private int dim1;
    private int dim2;
}
```

##### DIMVAR

El comando **DIMVAR** declara un arreglo, estableciendo la información de dimensión de la variable.

```java
case DIMVAR: {
    Var var = getVar((String) code.getValue1());
    int n = (int) code.getValue2();
    var.setDimension(n);
    if (n == 1) {
        int i = pop();
        var.setDim1(i);
    }
    if (n == 2) {
        int j = pop(), i = pop();
        var.setDim1(i);
        var.setDim2(j);
    }
}
```

##### PLACEHOLDER

El comando **PLACEHOLDER** hace crecer la pila hacia abajo, asignando nuevo espacio para variables y arreglos.

```java
case PLACEHOLDER: {
    Var var = getVar((String) code.getValue1());
    int n = (int) code.getValue2();
    if (n == 0) {
        push(0);
    }
    if (n == 1) {
        for (int i = 0; i < var.getDim1(); i++) {
            push(0);
        }
    }
    if (n == 2) {
        for (int i = 0; i < var.getDim1() * var.getDim2(); i++) {
            push(0);
        }
    }
}
```

##### Otros

Tipo de cálculo: Sacar de la pila una o dos veces, realizar el cálculo y volver a empujar el resultado a la pila.

Tipo de salto: Para los comandos de salto, verificar si se cumple la condición y cambiar el `eip` en consecuencia.

Llamada a función: Como se muestra a continuación

#### Procedimiento de llamada a función

Primero, antes de la llamada a función, los parámetros se empujan a la pila. Cada empuje de parámetro va seguido de un comando `RPARA`, que memoriza la dirección de la variable anterior.

```java
case RPARA: {
    int n = (int) code.getValue1();
    if (n == 0) {
        rparas.add(stack.size() - 1);
    } else {
        rparas.add(stack.get(stack.size() - 1));
    }
}
```

Segundo, la función `CALL`.

Memorizar el `eip`, la dirección superior de la pila y la información sobre la función (todo esto también se empujará a la pila). Luego actualizar la `varTable` y el `eip`, preparándose para ejecutar la función.

```java
case CALL: {
    Func func = funcTable.get((String) code.getValue1());
    retInfos.add(new RetInfo(eip, varTable, stack.size() - 1, func.getArgs(), func.getArgs(), nowArgsNum));
    eip = func.getIndex();
    varTable = new HashMap<>();
    callArgsNum = func.getArgs();
    nowArgsNum = 0;
}
```

Finalmente, cuando es `RET`, retornar.

Restaurar `eip` y `varTable` desde `RetInfo`, y borrar la nueva información empujada a la pila durante la llamada a función.

```java
case RET: {
    int n = (int) code.getValue1();
    RetInfo info = retInfos.remove(retInfos.size() - 1);
    eip = info.getEip();
    varTable = info.getVarTable();
    callArgsNum = info.getCallArgsNum();
    nowArgsNum = info.getNowArgsNum();
    if (n == 1) {
        stack.subList(info.getStackPtr() + 1 - info.getParaNum(), stack.size() - 1).clear();
    } else {
        stack.subList(info.getStackPtr() + 1 - info.getParaNum(), stack.size()).clear();
    }
}
```

#### Valor o Dirección

Empujar el valor o la dirección de una variable es importante y depende de la necesidad específica, lo cual se explicará cuando describa cómo generar el código.

La acción del comando es la siguiente (`getAddress` se utiliza para obtener la dirección de la variable anterior).

```java
case VALUE: {
    Var var = getVar((String) code.getValue1());
    int n = (int) code.getValue2();
    int address = getAddress(var, n);
    push(stack.get(address));
}
...
case ADDRESS: {
    Var var = getVar((String) code.getValue1());
    int n = (int) code.getValue2();
    int address = getAddress(var, n);
    push(address);
}
```

#### Generación de Código

El código se genera a partir del procedimiento de análisis sintáctico.

##### Declaración

No es necesario distinguir entre constantes y variables. Al declarar una variable, crear una nueva variable y hacer que apunte a la parte superior de la pila. Si tiene una inicialización, empujar los valores uno tras otro. Si no, añadir un comando `PLACEHOLDER` para empujar algo (yo empujo 0) a la pila para reservar el espacio.

##### Sentencia de Asignación

En esta situación, primero calcular y empujar la dirección de la variable a la parte superior de la pila. Luego analizar las expresiones. Después de eso, solo habrá dos números en la pila: la dirección y el valor. Asignar el valor a la dirección.

##### Sentencia de Control de Condición

Primero, generar etiquetas. Luego, colocar sentencias de salto en los lugares adecuados.

Las etiquetas para `if` y `while` se generarán y luego se almacenarán en una estructura tipo pila, como:

```java
whileLabels.add(new HashMap<>());
whileLabels.get(whileLabels.size() - 1).put("while", labelGenerator.getLabel("while"));
whileLabels.get(whileLabels.size() - 1).put("while_end", labelGenerator.getLabel("while_end"));
whileLabels.get(whileLabels.size() - 1).put("while_block", labelGenerator.getLabel("while_block"));
```

Tomemos `if` como ejemplo:

```java
if (word.typeEquals("IFTK")) {
    codes.add(new PCode(CodeType.LABEL, ifLabels.get(ifLabels.size() - 1).get("if")));
    ...
    analyseCond("IFTK");
    ...
    codes.add(new PCode(CodeType.JZ, ifLabels.get(ifLabels.size() - 1).get("else")));
    codes.add(new PCode(CodeType.LABEL, ifLabels.get(ifLabels.size() - 1).get("if_block")));
    analyseStmt();
    codes.add(new PCode(CodeType.JMP, ifLabels.get(ifLabels.size() - 1).get("if_end")));
    codes.add(new PCode(CodeType.LABEL, ifLabels.get(ifLabels.size() - 1).get("else")));
    if (word.typeEquals("ELSETK")) {
        getWord(); //else
        analyseStmt();
    }
    codes.add(new PCode(CodeType.LABEL, ifLabels.get(ifLabels.size() - 1).get("if_end")));
}
```

while:

```java
if (word.typeEquals("WHILETK")) {
    ...
    codes.add(new PCode(CodeType.LABEL, whileLabels.get(whileLabels.size() - 1).get("while")));
    ...
    analyseCond("WHILETK");
    ...
    codes.add(new PCode(CodeType.JZ, whileLabels.get(whileLabels.size() - 1).get("while_end")));
    codes.add(new PCode(CodeType.LABEL, whileLabels.get(whileLabels.size() - 1).get("while_block")));
    analyseStmt();
    ...
    codes.add(new PCode(CodeType.JMP, whileLabels.get(whileLabels.size() - 1).get("while")));
    codes.add(new PCode(CodeType.LABEL, whileLabels.get(whileLabels.size() - 1).get("while_end")));
    whileLabels.remove(whileLabels.size() - 1);
}

// break
if (word.typeEquals("BREAKTK")) {
    getWord();//break
    codes.add(new PCode(CodeType.JMP, whileLabels.get(whileLabels.size() - 1).get("while_end")));
 		...
}

// continue
if (word.typeEquals("CONTINUETK")) {
    getWord();//continue
    codes.add(new PCode(CodeType.JMP, whileLabels.get(whileLabels.size() - 1).get("while")));
    ...
} 
```

### Después de la Programación (After Coding)

Debido a algunos errores en tiempo de ejecución y falta de información, agregué y eliminé algunas instrucciones PCode. Al mismo tiempo, surgieron algunos problemas nuevos con la transmisión de direcciones y el cálculo de cortocircuito.

#### Definición Específica de Código

En las operaciones, `push()` significa colocar un valor en la parte superior de la pila. `pop()` significa eliminar el valor de la parte superior de la pila.

##### Tipo Común

| CodeType    | Value1              | Value2             | Operation                               |
| ----------- | ------------------- | ------------------ | --------------------------------------- |
| LABEL       | Label_name          | Set  a label       |                                         |
| VAR         | Ident_name          | Declare  a variety |                                         |
| PUSH        | Ident_name/Digit    | push(value1)       |                                         |
| POP         | Address             | Ident_name         | *value1 = value2                        |
| JZ          | Label_name          |                    | Jump if stack top is zero               |
| JNZ         | Label_name          |                    | Jump if stack top is not zero           |
| JMP         | Label_name          |                    | Jump unconditionally                    |
| MAIN        |                     |                    | Main function label                     |
| FUNC        |                     |                    | Function label                          |
| ENDFUNC     |                     |                    | End of function label                   |
| PARA        | Ident_name          | Type               | Parameters                              |
| RET         | Return value or not |                    | Function return                         |
| CALL        | Function name       |                    | Function call                           |
| RPARA       | Type                |                    | Get parameters ready for function call  |
| GETINT      |                     |                    | Get a integer and put it into stack top |
| PRINT       | String              | Para num           | Pop values and print.                   |
| DIMVAR      | Ident_name          | Type               | Set dimension info for array variety    |
| VALUE       | Ident_name          | Type               | Get the variety value                   |
| ADDRESS     | Ident_name          | Type               | Get the variety address                 |
| PLACEHOLDER |                     |                    | Push something to hold places           |
| EXIT        |                     |                    | Exit                                    |

| CodeType | Value1 | Value2 | Operation |
| -------- | ------ | ------ | --------- |
| ADD      |        |        | +         |
| SUB      |        |        | -         |
| MUL      |        |        | *         |
| DIV      |        |        | /         |
| MOD      |        |        | %         |
| CMPEQ    |        |        | ==        |
| CMPNE    |        |        | !=        |
| CMPGT    |        |        | >         |
| CMPLT    |        |        | <         |
| CMPGE    |        |        | >=        |
| CMPLE    |        |        | <=        |
| AND      |        |        | &&        |
| OR       |        |        | \|\|      |
| NOT      |        |        | !         |
| NEG      |        |        | -         |
| POS      |        |        | +         |

#### cálculo de cortocircuito (short circuit calculation)

Hay dos situaciones en las que se necesita el cálculo de cortocircuito:

```c
1. if(a&&b) // a is false
2. if(a||b) // b is true
```

Esta no fue una tarea fácil, y realmente pasé mucho tiempo resolviéndola.

Este es mi método:

Primero, al analizar `analyseLOrExp`, cada `analyseLAndExp` va seguido de un `JNZ`, que se utiliza para detectar si la condición es falsa. Si lo es, salta a la etiqueta del cuerpo del if. Al mismo tiempo, generé una etiqueta de condición, que está lista para `analyseLAndExp`.

```java
private void analyseLOrExp(ArrayList<Word> exp, String from) {
    ...
    for (...) {
        ...
        String label = labelGenerator.getLabel("cond_" + i);
        analyseLAndExp(exp1, from, label);
        codes.add(new PCode(CodeType.LABEL, label));
        if (...) {
            codes.add(new PCode(CodeType.OR));
        }
        if (...) {
            if (...) {
                codes.add(new PCode(CodeType.JNZ, ifLabels.get(ifLabels.size() - 1).get("if_block")));
            }
          ...
        }
        ...
    }
}
```

En `analyseLAndExp`, cada `analyseEqExp` va seguido de un `JZ`, que se utiliza para detectar si la condición es verdadera. Si lo es, salta a la etiqueta de condición que establecí anteriormente.

```java
private void analyseLAndExp(ArrayList<Word> exp, String from, String label) {
    ...
    for (...) {
        ...
        analyseEqExp(exp1);
        if (...) {
            codes.add(new PCode(CodeType.AND));
        }
        if (...) {
            if (...) {
                codes.add(new PCode(CodeType.JZ, label));
            } 
          ...
        }
    }
}
```

Mediante estos medios, se logra la evaluación de cortocircuito.

## Resumen y Reflexiones Summary

Primero, hablaré sobre la elección de PCode. Al elegir PCode, terminé todas las tareas del experimento de compilación mucho antes, sin pasar por el dolor que muchos sienten al elegir entre mips85+ o PCode+85. Personalmente, no quería sumergirme en la ansiedad por discutir calificaciones, ni quería gastar mucho tiempo investigando cómo generar mips y optimizar al máximo, porque no es algo que quiera hacer ni me guste. Por supuesto, generar PCode fue algo que encontré más interesante; no fue necesario seguir un sistema establecido (aunque esto podría no beneficiarme), sino que diseñé mis propias instrucciones PCode, generando instrucciones desde el descenso recursivo mientras escribía el intérprete de la máquina virtual. Entre ambos, modifiqué constantemente la generación de instrucciones y el intérprete, sintiendo que tenía un control total sobre este compilador que escribí con mis propias manos. Finalmente, lograr que la máquina virtual ejecute el código de manera completa y exitosa fue, en cambio, algo que me dio una gran sensación de logro.

En cuanto a por qué este documento está escrito en inglés, en primer lugar, quería describir el código, las clases y los objetos de manera más conveniente e intuitiva; en lugar de darle una explicación en chino a cada cosa, consideré que describirlo directamente en inglés resultaba más natural. En segundo lugar, quería practicar mi inglés mientras escribía la documentación, eso es todo. No pretendía causar molestias a los asistentes ni a quienes leen mi documento. Si es muy incómodo de leer, puede traducirlo al chino y luego leerlo; incluso después de usar un traductor como Youdao, se puede entender. Si no funciona, siempre puede consultarme personalmente. Por supuesto, el resumen final no lo escribiré en inglés.

Sobre algunas reflexiones, en las partes de análisis sintáctico y generación de código, comencé a escribir sin haber diseñado bien la arquitectura. Después de terminar, surgieron muchos errores, e incluso llegué a encontrar errores de la fase de análisis sintáctico durante la generación de código. Ya había reflexionado sobre esto varias veces en el curso de programación orientada a objetos; esto realmente no debería haber pasado. En futuros proyectos a gran escala, prestaré mucha más atención al diseño de la arquitectura. También quiero dar las gracias a mis compañeros zyq, lyx y dky por ayudarme durante mi proceso de depuración.

Finalmente, desde mi perspectiva personal, considero que las cuatro tareas de análisis léxico, análisis sintáctico, manejo de errores y generación de código me permitieron comprender a fondo la arquitectura de un compilador y completarlo por mí mismo. Para mí, esto ya estaba completo. Completé el experimento de compilación de manera bastante relajada y agradable (aunque la depuración durante las tareas de análisis sintáctico y generación de código no fue muy suave), logrando una gran sensación de logro y obteniendo muchos beneficios.
