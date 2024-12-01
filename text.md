# Введение

Перед прочтением данного руководства рекомендуется ознакомиться с общими сведениями изложенными в справке по Skynet++ в разделе "Введение", а так же общие сведения из справки по синтаксису.

В данном руководстве рассматривается внедрение **Skynet++** в **хост-программу**. В качестве хост-программы у нас будет небольшая RPG. В ней игрок может перемещаться по карте, открывать различные двери, собирать предметы из сундуков, разговаривать с NPC или сражаться с ними.

# Обзор кода хост-программы
Давайте рассмотрим код хост-программы. Помимо стандартной инициализации 3D в Blitz3D и основного цикла UpdateWorld-RenderWorld в нём комментариями выделены следующие разделы:

- Misc - Вспомогательный код, который содержит функцию дельта-тайминга для плавного обновления в игре, вне зависимости от производительности
- Main Camera - Создание и настройка камеры
- Light - Создание глобального источника света
- Player - Персонаж игрока с помощью которого мы будем взаимодействовать с окружением
- Map - Функции для создания элементов карты, а так же плейсхолдеров - точек на карте в которых мы будем расставлять различные предметы
- Door - Игровые двери. Сейчас они открываются безусловно при соприкосновении игрока с ней
- NPC - Игровые персонажи с которыми мы будем разговаривать или сражаться
- Chests - Сундуки с предметами в игре. При взаимодействии с ними по нажатию клавиши "пробел" мы открываем их и получаем какой либо предмет (пока что только один вид предмета)
- Items - Инвентарь персонажа
- Dialog - Этот раздел содержит реализацию диалогов в игре
- Battle - Всё что связано с фазой боя в игре
- Game - Тут реализована глобальная логика фаз игры. Фаз всего три: Диалог - разговор с персонажем; Бой - бой с персонажем, Приключение - ходьба по карте

### Карта
В разделе "Game initialization" происходит загрузка стартовой карты "map.txt". Давайте рассмотрим файл карты подробнее:

```
**********************
*....................*
*....................*
*....................*
*...................@d
*....................*
*....................*
*.............x......*
*....................*
*....................*
*.............c......*
*....................*
*....................*
**********************
entities

ground 0 255 0
door d 0 test_map2.txt
chest c "Gold" 100
npc x "Wise Old Man" start
```

В этом файле карта описывается с помощью специальных символов:
- Символ ```*``` - означает стену
- Символ ```.``` - означает пробел или пустое пространство
- Символ ```@``` - означает точку где появится игрок при загрузке карты
- Все остальные символы используются для расстановки плейсхолдеров - точек на карте где нужно что-то разместить

Описание карты заканчивается ключевым словом entities. После этого слова начинается настройка объектов карты. Объекты карты настраиваются следующими командами:

**ground ```r``` ```g``` ```b```**

Устанавливает цвет земли карты. Параметры:
- ```r``` - красный компонент цвета
- ```g``` - зеленый компонент цвета
- ```b``` - синий компонент цвета

**door ```map_placeholder``` ```opened``` ```map_name```**

Добавляет на карту дверь. Параметры:
- ```map_placeholder``` - имя плейсхолдера где нужно разместить дверь
- ```opened``` - Указывает открыта ли дверь. 0 - закрыта, 1 - открыта
- ```map_name``` - Имя файла карты, которую нужно загрузить при открытии двери


**chest ```map_placeholder``` ```item_name``` ```item_count```**

Добавляет на карту сундук. Параметры:
- map_placeholder - имя плейсхолдера где нужно разместить сундук
- item_name - Имя предмета который получает игрок при открытии сундука
- item_count - Количество единиц предмета item_name

**npc ```map_placeholder``` ```npc_name``` ```dialog_id```**

Добавляет на карту сундук. Параметры:
- ```map_placeholder``` - имя плейсхолдера где нужно разместить NPC
- ```npc_name``` - Имя персонажа
- ```dialog_id``` - ID диалога в файле диалогов (о нём ниже по тексту). Этот диалог начнётся при взаимодействии с NPC

### Диалоги
Диалоги в игре написаны в файле ```dialogs.txt```. Он имеет следующие команды для описания диалогов:

**#dialog ```id```**

Означает начало диалога. Все сообщения описанны после этого слова будут относиться к данному диалогу. Параметры:
- ```id``` - ID диалога, по которому его можно найти (этот id указывается при настройке NPC)

**#option  ```dialog_id```**
Вариант ответа на последнее сообщение. При выборе этого ответа выполняется переход на диалог с соответствующим `dialog_id`. Следующая строка после этой команды будет взята в качестве выбираемого варианта ответа.

Пример:

```
#dialog start
Be careful!
Monsters roaming here

#option what_they_looking_for
What they looking for?

#option how_many
How many monsters here?

#dialog what_they_looking_for
I don't know.
I hid here when they came in
Help me. Defeat the monsters, and I'll give you reward!

#dialog how_many
At least two
One here and a bigger one behind the door
```

# Внедрение Skynet++

## Включение библиотеки в игру

Для того чтобы внедрить Skynet++ скопируем в папку с игрой файлы:

```
Skynet++.bb
Skynet++ Compiler.bb
Skynet++ Base Lib.bb
```

После чего добавим строку в файле ```Game.bb```:
```
Include "Skynet++.bb"
```

Откроем файл ```Skynet++.bb``` и найдём в нём функцию ```SE_InvokeGlobalFunction```
По умолчанию в репозитории есть функции написанные для работы примеров, и функция имеет следующий вид:

```
Function SE_InvokeGlobalFunction(FunctionName$)
	If SE_BL_Math(FunctionName$) Then Return
	If SE_BL_Str(FunctionName$) Then Return
	If SE_BL_Array(FunctionName$) Then Return

	Include "Example Functions.bb"
End Function
```

Т.к. функции из примеров мы использовать не будем, то можем удалить строку ```Include "Example Functions.bb"```.

Далее в разделе инициализации программы нужно вызвать функцию ```SE_Init()```

На этом подключение библиотеки завершено

## Реализация скриптов для сущностей

Теперь давайте подумаем для чего мы вообще будем использовать скрипты...

При взаимодействии игрока с определёнными предметами или персонажами происходят различные действия:
- При открытии двери - она открывается
- При открытии сундука - игрок получает предметы
- При взаимодействии с NPC - начинается диалог
- При выборе варианта ответа диалога - начинается другой диалог
- При встрече с врагом - начинается бой

Всем вышеперечисленным действиям предшествуют события (при открытии, при взаимодействии, при выборе), в момент которых можно проверить условия выполнения этих действий. Проверку этих условий, а так же действия после них мы и будем описывать в скриптах. Нам нужно спланировать события.

Давайте начнём с двери...

### Двери

#### Планирование структуры скрипта

Сейчас при взаимодействии игрока с дверью, она сразу открывается. В большинстве игр для открытия дверей требуется ключ или выполнение того или иного действия. Очевидно, что скрипт, который мы будем писать, должен будет проверить необходимые условия открытия двери (например наличие предметов в инвентаре) и после этого решить открыть дверь или нет. Назовём это событие **on_before_open**. Попробуем создать скрипт и описать его текст...

Для начала создадим папку Scripts и в ней создадим файл door.txt. Напишем следующий код:

```
def on_before_open()
    return false;
end
```

Здесь мы описали пользовательскую функцию **on_before_open**, которая возвращает false и наша дверь по сути будет всегда закрыта. Чуть позже мы усложним это условие, но пока нам хватит и этого.

Кроме того, нам в дальнейшем потребуется функция **on_after_open**, которая будет что-то делать когда дверь всё же открылась.

#### Изменение хост-программы

Итак, у нас есть скрипт и нам теперь как-то нужно чтобы игра знала, что с ним делать. Для этого нам нужно модифицировать ```Game.bb``` и добавить в дверь обработку условия скрипта.

Для этого изменим тип Door:

```
Type Door
    Field Entity
    Field Opened
    Field Map$

    Field on_before_open.SE_FuncPtr
    Field on_after_open.SE_FuncPtr
End Type
```

Мы добавили строку ```Field on_before_open.SE_FuncPtr``` и ```Field on_after_open.SE_FuncPtr``` где будет содержаться пользовательская функция скрипта, которая будет вызываться при возникновении события.

Отлично. Функция скрипта у нас есть. Теперь нам нужно сделать саму проверку события.

Событие взаимодействия игрока с дверью описано в функции ```UpdateDoors```. Функция имеет вид:

```
Function UpdateDoors()
    Local DoorEntity = EntityCollided ( Player, CT_Door )

    For Door.Door = Each Door
        If Door\Entity = DoorEntity Then
            SetDoorOpened(Door, True)
        End If
    Next
End Function
```

Здесь проверяется столкновение игрока с дверью и последующее её открытие. Как нам добавить проверку условий с помощью скрипта? Ниже приведен код модифицированной функции ```UpdateDoors```:

```
Function UpdateDoors()
    Local DoorEntity = EntityCollided ( Player, CT_Door )

    For Door.Door = Each Door
        If Door\Entity = DoorEntity Then
            If Door\on_before_open <> Null Then
                SE_CallUserFunction(Door\on_before_open)
                SetDoorOpened(Door, SE_ValueToInt(SE_RETURN_VALUE))
            Else
                SetDoorOpened(Door, True)
            End If
        End If
    Next
End Function
```



Данный код действует следующим образом:
1. В первую очередь идёт проверка на наличие самой функции-обработчика события ```on_before_open```. Если она не найдена то срабатывает поведение по умолчанию - открывается дверь
2. Если пользовательская функция ```on_before_open``` всё же найдена то происходит её выполнение с помощью функции ```SE_CallUserFunction```. Результат выполнения ```on_before_open``` помещается в переменную ```SE_RETURN_VALUE```
4. Далее мы приводим значение ```SE_RETURN_VALUE``` к целочисленному типу с помощью функции ```SE_ValueToInt``` и передаём его функции ```SetDoorOpened```, которая открывает дверь в зависимости от результата

Кроме того изменим функцию ```SetDoorOpened``` и добавим вызов функции ```on_after_open```

```
Function SetDoorOpened(Door.Door, Opened)
    If Opened = True Then
        If Door\on_after_open <> Null Then
            SE_CallUserFunction(Door\on_after_open)
        End If

        If Door\Map$ <> "" Then
            DebugLog Door\Map$
            LoadMap(Door\Map$)
            Return
        Else
            HideEntity Door\Entity
        End If
    Else
        ShowEntity Door\Entity
    End If

    Door\Opened = Opened
End Function
```

#### Загрузка скрипта

Приведённый выше код уже готов к работе, однако мы не загрузили сам скрипт. Для того чтобы добавить загрузку скрипта, найдём в файле ```Game.bb``` функцию ```LoadMap```. Нас интересует секция ```Case "door"```. Здесь и будет происходить загрузка скрипта:

```
Case "door"
    PlaceholderName$ = NextToken()
    Local Opened = Int(NextToken())
    Local Map$ = NextToken()

    For Placeholder.MapPlaceholder = Each MapPlaceholder
        If Placeholder\Name = PlaceholderName$ Then
            CreateDoor(Placeholder\x, Placeholder\z, Opened, Map)
        EndIf
    Next
```

Добавим в эту функцию загрузку скрипта:
```
Case "door"
    PlaceholderName$ = NextToken()
    Local Opened = Int(NextToken())

    Local Script.SE_Script = SE_LoadScriptText.SE_Script("Scripts\" + NextToken())

    Local Map$ = NextToken()

    For Placeholder.MapPlaceholder = Each MapPlaceholder
        If Placeholder\Name = PlaceholderName$ Then
            Local Door.Door = CreateDoor(Placeholder\x, Placeholder\z, Opened, Map)

            If Script <> Null Then
                Door\on_before_open = SE_FindFunc(Script, "on_before_open")
                Door\on_after_open = SE_FindFunc(Script, "on_after_open")
            End If
        EndIf
    Next
```
С помощью функци `SE_FindFunc` мы находим функции для обработки событий перед и после открытия двери и помещаем их в объект двери.

Теперь нам осталось только поправить файл карты и изменить строку ```door d 0 test_map2.txt``` на ```door d 0 door.txt test_map2.txt```

Запустим игру и проверим работу скрипта. Теперь дверь не открывается. Скрипт работает!

#### Подключение глобальных функций

Усложним пример. Теперь мы хотим чтобы дверь открывалась при наличии ключа. Для этого мы должны иметь доступ к инвентарю игрока. Чтобы реализовать доступ, перейдём в раздел ```;Items``` и напишем функцию проверки наличия предметов в инвентаре:

```
Function CheckItem(Name$)
    For Item.Item = Each Item
        If Item\Name = Name And Item\Count > 0 Then
            Return True
        End If
    Next
End Function
```

Теперь чтобы мы могли использовать эту функцию в скриптах, нам нужно её подключить. Делается это путём модификации ```SE_InvokeGlobalFunction```. Перейдём в неё и модифицируем её:

```
Function SE_InvokeGlobalFunction(FunctionName$)
	If SE_BL_Math(FunctionName$) Then Return
	If SE_BL_Str(FunctionName$) Then Return
	If SE_BL_Array(FunctionName$) Then Return

	Select FunctionName
		Case "check_item"
			SE_ReturnInt(CheckItem(SE_StringArg(0, "")))
	End Select
End Function
```

Здесь мы вызываем объявленную выше по тексту функцию ```CheckItem```, передавая в неё первый параметр, с помощью функции ```SE_StringArg``` и возвращаем значение функции в качестве целочисленного значения используя функцию ```SE_ReturnInt```. Функции начинающиеся на `SE_Return...` предназначены для возврата результата выполнения глобальных функций.

Теперь эта функция доступна в скриптах. Изменим код скрипта door.txt:

```
def on_before_open()
    return check_item("Key")
end
```

Запустим игру...
Как мы можем видеть - дверь всё ещё не открывается...
В карте ```test_map.txt``` присутствует сундук в котором можно найти золото. Поскольку золото нам не поможет выбраться из комнаты, изменим ```chest c "Gold" 100``` на ```chest c "Key" 1```

Запустим игру снова...
Теперь мы можем взять ключ в сундуке рядом и открыть дверь. Интеграция Skynet++ для дверей готова!

### Сундук

На данный момент мы можем открыть любой игровой сундук и получить только один предмет в определённом количестве. Но что если нам нужно сделать так, чтобы сундук открывался только с помощью определённого ключа? Или чтобы игрок мог получить из сундука несколько предметов? Мы можем сделать сундуки более функциональными с помощью скриптов.

#### Планирование структуры скрипта

Давайте рассмотрим взаимодействие с сундуком. Когда игрок взаимодействует с сундуком - он должен открыться или остаться закрытым. Если же сундук всё же открылся - игрок должен получить предметы. Итак выделим два события:
1. ```on_before_open``` - Так же как и в случае с дверью, оно возникает при взаимодействии с сундуком и проверяет условия необходимые для его открытия (наличие ключа)
2. ```on_after_open``` - Если в ```on_before_open``` условие всё таки вернуло `true` то выполняется эта функция-обработчик и выдает игроку предметы

#### Изменение хост-программы

Добавим поля функций-обработчиков в тип сущности сундука:

```
Type Chest
    Field Entity, Top
    Field ItemName$, ItemCount%
    Field Opened

    Field on_before_open.SE_FuncPtr
    Field on_after_open.SE_FuncPtr
End Type
```

Выполнение вышеописанных функций удобнее всего внести в функцию ```OpenChest```.

```
Function OpenChest(Chest.Chest)
    If Not Chest\Opened Then
        If Chest\on_before_open <> Null Then
            SE_CallUserFunction(Chest\on_before_open)
            Chest\Opened = SE_ValueToInt(SE_RETURN_VALUE)
        Else
            Chest\Opened = True
        End If

        If Chest\Opened Then
            If Chest\on_after_open <> Null Then
                SE_CallUserFunction(Chest\on_after_open)
            End If

            EntityColor Chest\Top, 0, 0, 0
        End If
    End If
End Function
```

Здесь мы с помощью функции ```on_before_open``` проверяем условия выполнения сундука и если они выполнены (или функция отсутствует), то мы выполняем ```on_after_open``` где мы выдаём игроку предметы или производим какие-то действия.


#### Подключение глобальных функций

Для того чтобы выдать игроку предмет через скрипт нам нужна функция, которую мы могли бы использовать из скрипта. Для этого добавим в разделе ```;Items``` функцию ```GiveItem```, с помощью которой будем выдавать предметы.

```
Function GiveItem(Name$, Amount)
    For Item.Item = Each Item
        If Item\Name = Name Then
            Item\Count = Item\Count + Amount
            Return True
        End If
    Next

    Item = New Item
    Item\Name = Name
    Item\Count = Amount
End Function
```

Некоторые предметы являются расходным материалом, поэтому нам так же понадобится функция которая вычитает определённое количество предметов из инвентаря. Назовём её ```TakeItem```.

```
Function TakeItem(Name$, Amount)
    For Item.Item = Each Item
        If Item\Name = Name Then
            Item\Count = Item\Count - Amount
            If Item\Count <= 0 Then Delete Item;
            Exit
        End If
    Next
End Function
```

Так же добавим функцию для проверки наличия необходимого количества предметов ```EnoughItems```. Она нам пригодится в будущем.

```
Function EnoughItems(Name$, Amount)
    For Item.Item = Each Item
        If Item\Name = Name Then
            Return Item\Count >= Amount
        End If
    Next

    Return False
End Function
```

Подключим эти функции в ```SE_InvokeGlobalFunction```:

```
Function SE_InvokeGlobalFunction(FunctionName$)
    If SE_BL_Math(FunctionName$) Then Return
    If SE_BL_Str(FunctionName$) Then Return
    If SE_BL_Array(FunctionName$) Then Return

    Select FunctionName
        Case "check_item"
            SE_ReturnInt(CheckItem(SE_StringArg(0, "")))

        case "give_item"
            GiveItem(SE_ToStringArg(0, ""), SE_ToIntArg(1, 0))

        case "take_item"
            TakeItem(SE_ToStringArg(0, ""), SE_ToIntArg(1, 0))

        case "enough_items"
            SE_ReturnInt(EnoughItems(SE_ToStringArg(0, ""), SE_ToIntArg(1, 0)))
    End Select
End Function
```

Функция ```GiveItem``` получит параметр ```Name``` по индексу 0 и ```Amount``` по индексу 1, после чего вызовет саму функцию. Остальные функци получают параметры таким же образом, а ```enough_items``` ещё и возвращает результат проверки.

#### Загрузка скрипта

Немного исправим функцию CreateChest потому что нам уже не нужно указывать имя и количество предметов.

```
Function CreateChest.Chest(X#, Z#, R=255, G=255, B=0)
    Local Chest.Chest = New Chest

    Chest\Entity = CreateCube()
    PositionEntity Chest\Entity, X, 0, Z
    EntityColor Chest\Entity, R, G, B
    EntityType Chest\Entity, CT_Chest

    Chest\Top = CreateCube(Chest\Entity)
    EntityColor Chest\Top, 150, 150, 150
    ScaleEntity Chest\Top, .8, .8, .8
    MoveEntity Chest\Top, 0, .4, 0

    Return Chest

End Function
```

Далее вернёмся в уже знакомую нам функцию ```LoadMap``` и изменим блок ```Case "chest"```. Так как предметы мы теперь будем выдавать игроку через скрипты, то нам при загрузке карты вместо указания типа предмета и его количества можно указать только имя файла скрипта.

```
Case "chest"
    PlaceholderName$ = NextToken()
    Script.SE_Script = SE_LoadScriptText.SE_Script("Scripts\" + NextToken())

    For Placeholder.MapPlaceholder = Each MapPlaceholder
        If Placeholder\Name = PlaceholderName$ Then
            Local Chest.Chest = CreateChest(Placeholder\x, Placeholder\z)

            If Script <> Null Then
                Chest\on_before_open = SE_FindFunc(Script, "on_before_open")
                Chest\on_after_open = SE_FindFunc(Script, "on_after_open")
            End If
        EndIf
    Next
```

Теперь создадим скрипт ```chest.txt``` в папке ```Scripts```, со следующим содержанием:

```
def on_after_open()
    give_item("Gold", 100)
    give_item("Key", 1)
end
```

Описанный выше скрипт сразу же откроет сундук и выдаст игроку ключ от двери и 100 золотых, поскольку мы не описали событие ```on_before_open```. Его мы добавим позднее.

Нам осталось только отредактировать файл карты ```test_map.txt``` и исправить строку ```chest c "Key" 1``` на ```chest c chest.txt```, чтобы присоединить к сундуку скрипт

Запускаем игру и вуаля! Теперь при взаимодействии с сундуком, мы получаем сразу два предмета: золото и ключ от двери.

Предположим что нам нужно, чтобы сундук открывался только с помощью ключа. Отредактируем скрипт который написали выше:

```
def on_before_open()
    return check_item("Chest Key")
end

def on_after_open()
    take_item("Chest Key", 1)
    give_item("Gold", 100)
    give_item("Key", 1)
end
```

Теперь сундук откроется только при наличии предмета "Chest Key". Однако нам нужно добавить ещё один сундук где игрок получит этот предмет. Для этого изменим файл карты ```test_map.txt```. На месте любой точки на карте вставим букву "k" и после строки ```chest c chest.txt``` вставим строку:

```
chest k chest2.txt
```

Теперь создадим скрипт ```chest2.txt```

```
def on_after_open()
    give_item("Chest Key", 1)
end
```

Готово. Теперь прежде чем мы открыть сундук с ключом от двери, нам нужно забрать ключ от сундука в другом сундуке. А так же использованный предмет тут же удалится из инвентаря. Геймплей стал немного более многоступенчатым :)

## Оптимизация

Перед тем как мы продолжим внедрять скрипты в игру дальше, нужно отметить что сейчас у нас есть две проблемы:
1. Скрипты загружаются каждый раз при загрузке уровня, и таким образом всё больше и больше заполняют память.
2. Когда мы возвращаемся на локацию снова - сундуки снова закрыты и мы можем получать предметы снова и снова. Это ломает геймплей и даёт возможность "фармить" золото ключи.

### Кэширование скриптов

Для решения первой проблемы, нам поможет кэш скриптов. Он будет проверять загружался ли скрипт ранее и в случае если он уже был загружен - возвращать уже загруженный скрипт, вместо создания нового.

Перейдём в конец раздела ```;Misc``` и напишем код:

```
Const ScriptsPath$ = "Scripts\"

Type CachedScript
    Field FileName$
    Field Script.SE_Script
End Type

Function LoadScript.SE_Script(FileName$)
    If FileName = "" Then Return

    For CachedScript.CachedScript = Each CachedScript
        If CachedScript\FileName = FileName Then
            Return CachedScript\Script;
        End If
    Next

    CachedScript.CachedScript = New CachedScript
    CachedScript\FileName = FileName
    CachedScript\Script = SE_LoadScriptText(ScriptsPath + FileName)

    If SE_ERROR Then
        DebugLog "Failed to load script: " + FileName + " loaded"
        DebugLog SE_ERROR_MESSAGE
        Return Null
    EndIf

    DebugLog "Script loaded: " + FileName

    Return CachedScript\Script
End Function
```

Далее в функции ```LoadMap``` заменим все вызовы ```SE_LoadScriptText``` на ```LoadScript```:

```
Local Script.SE_Script = LoadScript.SE_Script(NextToken())
```

Теперь если мы запустим игру в режиме отладки и посмотрим в консоль отладки, то увидим там, что сообщение об успешной загрузке скрипта появляется только единожды. Кроме того если мы допустим ошибку в скрипте, то в консоль отладки попадёт сообщение о синтаксической ошибке, что иногда может быть полезным.

### Системные события

Для решения второй проблемы, нам потребуется определить некоторые системные события. Мы уже описали ранее события для игровых объектов. Это были события относящиеся к взаимодействию с предметами . Однако есть события которые происходят с предметами вне зависимости от взаимодействия игрока с ними. Это например, событие когда предмет создан, или событие, которое происходит при каждом обновлении игрового цикла. Давайте назовём их как ```on_create``` (когда объект создаётся) и ```on_update``` (при каждом обновлении игрового цикла).

#### Создание функции событий

Добавим в уже созданные скрипты функции событий

```
def on_create(objectHandle)
    //...Какой-то код
end

def on_update(objectHandle)
    //...Какой-то код
end
```

Что-то новенькое. Теперь в фунциях есть некий формальный параметр ```objectHandle```. В этот параметр будет передаваться ссылка на объект, с которым происходит событие. В дальнейшем мы будем передавать его в глобальные функции, которые что-то делают с объектами.

#### Изменения в хост-программе

Дополним типы Двери и Сундука полями с функциями этих событий:

```
Type Door
    Field Entity
    Field Opened
    Field Map$

    Field on_before_open.SE_FuncPtr

    Field on_create.SE_FuncPtr
    Field on_update.SE_FuncPtr
End Type
```

```
Type Chest
    Field Entity, Top
    Field ItemName$, ItemCount%
    Field Opened

    Field on_before_open.SE_FuncPtr
    Field on_after_open.SE_FuncPtr

    Field on_create.SE_FuncPtr
    Field on_update.SE_FuncPtr
End Type
```

Создание объектов у нас происходит в функции ```LoadMap```, там мы и будем вызывать функции ```on_create``` для игровых объектов. Кроме того, там же мы должны получить ссылки на функции обработчики для системных событий


```
            Case "door"
                PlaceholderName$ = NextToken()
                Local Opened = Int(NextToken())

                Local Script.SE_Script = LoadScript.SE_Script(NextToken())

                Local Map$ = NextToken()

                For Placeholder.MapPlaceholder = Each MapPlaceholder
                    If Placeholder\Name = PlaceholderName$ Then
                        Local Door.Door = CreateDoor(Placeholder\x, Placeholder\z, Opened, Map)

                        If Script <> Null Then
                            Door\on_before_open = SE_FindFunc(Script, "on_before_open")
                            Door\on_after_open = SE_FindFunc(Script, "on_after_open")
                            Door\on_create = SE_FindFunc(Script, "on_create")
                            Door\on_update = SE_FindFunc(Script, "on_update")
                        End If
                    EndIf
                Next

            Case "chest"
                PlaceholderName$ = NextToken()
                Script.SE_Script = LoadScript.SE_Script(NextToken())

                For Placeholder.MapPlaceholder = Each MapPlaceholder
                    If Placeholder\Name = PlaceholderName$ Then
                        Local Chest.Chest = CreateChest(Placeholder\x, Placeholder\z)

                        If Script <> Null Then
                            Chest\on_before_open = SE_FindFunc(Script, "on_before_open")
                            Chest\on_after_open = SE_FindFunc(Script, "on_after_open")
                            Chest\on_create = SE_FindFunc(Script, "on_create")
                            Chest\on_update = SE_FindFunc(Script, "on_update")
                        End If
                    EndIf
                Next
```

Пока мы ещё не вызываем функцию ```on_create```. Перейдём в конец раздела ```;Misc``` и напишем универсальную функцию для вызова обработчиков ```CallMethod```:

```
Function CallMethod(Method.SE_FuncPtr, ObjectHandle = 0)
    If Method = Null Then
        Return
    End If

    If ObjectHandle <> 0 Then
        SE_AddIntArg(ObjectHandle)
    End If

    SE_CallUserFunction(Method)
End Function
```

Эта функция проверяет, что такой метод вообще существует. Далее при вызове метода она пытается передать значение в формальный параметр ```objectHandle``` c помощью функции ```SE_AddIntArg```.  Функции ```SE_AddNullArg```, ```SE_AddIntArg```, ```SE_AddFloatArg```, ```SE_AddStringArg```, ```SE_AddPointerArg``` поочерёдно помещают значения параметров функции перед её вызовом. Например:

Допустим, в скрипте у нас есть функция вычисляющая сумму трёх чисел ```sum```:

```
def sum(a, b, c)
    return a + b + c
end
```

Чтобы перед её вызовом из хост-программы передать значения в параметры (например: a=1, b=2, c=3) мы должны несколько раз вызвать функциию ```SE_AddIntArg```.

```
Include "Skynet++.bb"

Local Script.SE_Script = SE_LoadScriptText("sum.txt")
Local Sum = SE_FindFunc(Script, "sum")

SE_AddIntArg(1)
SE_AddIntArg(2)
SE_AddIntArg(3)
SE_CallUserFunction(Sum)

Print SE_RETURN_VALUE\IntValue
```

Данный код BlitzBasic эквивалентен вызову функций в скрипте:

```
sum(1, 2, 3)
```

Таким образом - каждый вызов функций ```SE_AddXXXArg``` помещает значение с типом ```XXX``` в каждый следующий формальный параметр и чтобы получить к ним доступ из функции скрипта, у неё должно быть ровно столько же параметров, сколько и вызовов функций ```SE_AddXXXArg```. Если вызовов ```SE_AddXXXArg``` больше чем параметров функций - то это не является ошибкой. Просто они не будут использованы при вызове.

Теперь давайте добавим вызов ```CallMethod``` в ```LoadMap```: 


```
            Case "door"
                PlaceholderName$ = NextToken()
                Local Opened = Int(NextToken())

                Local Script.SE_Script = LoadScript.SE_Script(NextToken())

                Local Map$ = NextToken()

                For Placeholder.MapPlaceholder = Each MapPlaceholder
                    If Placeholder\Name = PlaceholderName$ Then
                        Local Door.Door = CreateDoor(Placeholder\x, Placeholder\z, Opened, Map)

                        If Script <> Null Then
                            Door\on_before_open = SE_FindFunc(Script, "on_before_open")
                            Door\on_after_open = SE_FindFunc(Script, "on_after_open")
                            Door\on_create = SE_FindFunc(Script, "on_create")
                            Door\on_update = SE_FindFunc(Script, "on_update")

                            CallMethod(Door\on_create, Handle(Door))
                        End If
                    EndIf
                Next

            Case "chest"
                PlaceholderName$ = NextToken()
                Script.SE_Script = LoadScript.SE_Script(NextToken())

                For Placeholder.MapPlaceholder = Each MapPlaceholder
                    If Placeholder\Name = PlaceholderName$ Then
                        Local Chest.Chest = CreateChest(Placeholder\x, Placeholder\z)

                        If Script <> Null Then
                            Chest\on_before_open = SE_FindFunc(Script, "on_before_open")
                            Chest\on_after_open = SE_FindFunc(Script, "on_after_open")
                            Chest\on_create = SE_FindFunc(Script, "on_create")
                            Chest\on_update = SE_FindFunc(Script, "on_update")

                            CallMethod(Chest\on_create, Handle(Chest))
                        End If
                    EndIf
                Next
```

#### Подключение глобальных функции

Для того чтобы управлять состоянием сундуков или дверей из скрипта, нам потребуются глобальные функции. Назовём их ```set_door_state``` и ```set_chest_state```.

```
Function SetDoorState(DoorHandle, Opened)
    Local Door.Door = Object.Door(DoorHandle)

    If Door <> Null Then
        Door\Opened = Opened

        If Door\Opened Then
            HideEntity Door\Entity
        Else
            ShowEntity Door\Entity
        End If
    End If
End Function
```

```
Function SetChestState(ChestHandle, Opened)
    Local Chest.Chest = Object.Chest(ChestHandle)

    If Chest <> Null Then
        Chest\Opened = Opened
        SetChestOpened(Chest)
    End If
End Function
```

Теперь добавим функции в ```SE_InvokeGlobalFunction```

```
Function SE_InvokeGlobalFunction(FunctionName$)
    If SE_BL_Math(FunctionName$) Then Return
    If SE_BL_Str(FunctionName$) Then Return
    If SE_BL_Array(FunctionName$) Then Return

    Select FunctionName
        Case "check_item"
            SE_ReturnInt(CheckItem(SE_StringArg(0, "")))

        case "give_item"
            GiveItem(SE_ToStringArg(0, ""), SE_ToIntArg(1, 0))

        case "take_item"
            TakeItem(SE_ToStringArg(0, ""), SE_ToIntArg(1, 0))

        case "enough_items"
            SE_ReturnInt(EnoughItems(SE_ToStringArg(0, ""), SE_ToIntArg(1, 0)))

        case "set_door_state"
            SetDoorState(SE_ToIntArg(0, 0), SE_ToIntArg(1, 0))

        case "set_chest_state"
            SetChestState(SE_ToIntArg(0, 0), SE_ToIntArg(1, 0))
    End Select
End Function
```

#### Изменение скриптов

Давайте теперь перепишем скрипты для сундуков:

Начнём с того в котором мы берём ключ от сундука ```chest2.txt```

```
global chest_opened = false

def on_after_open()
    give_item("Chest Key", 1)
    chest_opened = true
end

def on_create(objectHandle)
    set_chest_state(objectHandle, chest_opened)
end

def on_update(objectHandle)
    //...Какой-то код
end
```

Здесь мы задаём глобальную переменную ```chest_opened``` которая устанавливается как ```true``` когда мы открываем сундук. Поскольку скрипты не загружаются снова, каждый раз когда мы загружаем карту, то значение этой переменной сохраняется и доступно из любой функции скрипта. При первой загрузке карты, состояние сундука установится как "закрыто". При повторной загрузке карты, она состояние уже будет установлено как "открыто", поскольку проход через дверь подразумевает открытие сундука. 

Давайте внесём схожие изменения в другой сундук:

```
global chest_opened = false

def on_before_open()
    return check_item("Chest Key")
end

def on_after_open()
    take_item("Chest Key", 1)
    give_item("Gold", 100)
    give_item("Key", 1)
    chest_opened = true
end

def on_create(objectHandle)
    set_chest_state(objectHandle, chest_opened)
end

def on_update(objectHandle)
    //...Какой-то код
end
```

Для того чтобы проверить двери, добавим ещё одну дверь на карте:

```
**********************
*....................*
*....................*
*....................*
*.............a.....@d
*....................*
*....................*
*....k........x......*
*....................*
*....................*
*.............c......*
*....................*
*....................*
**********************
entities

ground 0 255 0
door d 0 door.txt test_map2.txt
chest c chest.txt
chest k chest2.txt
npc x "Wise Old Man" start
door a 0 door2.txt
```

И напишем её скрипт по аналогии с сундуками:

```
global opened = false

def on_after_open()
    opened = true
end

def on_create(objectHandle)
    set_door_state(objectHandle, opened)
end
```

На этом мы закончим с оптимизацией.

## Продолжение реализации скриптов для сущностей

Продолжим дальнейшее внедрение скриптов для NPC и Диалогов

### NPC

Для того чтобы "оживить" NPC нам нужно дать им две возможности: перемещаться в пространстве и говорить. Последнее будет описано в следующем разделе этого туториала, а пока дадим им возможность перемещаться.

#### Планирование структуры скрипта

Персонаж в игре обычно имеет своё поведение, которое может изменяться в тех или иных игровых ситуациях. Например, персонаж может патрулировать территорию, искать игрока, гнаться за ним и так далее... Удобнее всего такое поведение реализовать с помощью отдельных функций поведения, которые мы будем переключать при определённых условиях.
Так же нам понадобятся функции обрабатывающие события до и после появления персонажа на карте. И последнее - функция для обработки смерти персонажа, которая будет вызываться когда персонаж умрёт.

Как будет выглядеть наш скрипт:

```
def on_before_spawn()
    //Перед появлением. Если функция возвращет true - персонаж создаётся
    return true
end

def on_after_spawn(thisObject)
    //После появления. Функция будет устанавливать начальные параметры персонажа (если нужно)
end

def on_die(thisObject)
    //При сметри. Когда здоровье персонажа опустится до 0 - будет выполнена эта функция
end

def state_idle(thisObject)
    //Состояние персонажа без дела
end

def state_patrol(thisObject)
    //Состояние патрулирования
end

def state_chase_player(thisObject)
    //Состояние погони за 
end
```

Функции начинающиеся со `state_` будут реализовывать всё многообразие состояний персонажа. 

#### Изменение хост-программы

В хост-программе, в типе объекта NPC, мы добавим несколько полей

```
    Field Script.SE_Script
    Field Instance.SE_Instance

    Field on_die.SE_FuncPtr

    Field CurrentState.SE_FuncPtr
```

Тип будет выглядеть вот так:

```
Type NPC
    Field Name$
    Field Entity
    Field IsEnemy

    Field MovementSpeed#, MovementX#, MovementZ#, MovementTime#

    Field HP#
    Field AttackDamage#

    Field DialogId$

    Field Script.SE_Script
    Field Instance.SE_Instance

    Field on_die.SE_FuncPtr

    Field CurrentState.SE_FuncPtr
End Type
```

Для чего нужны эти поля:

- `Script.SE_Script` - это поле в котором мы будем хранить ссылку на скрипт которым будет обрабатываться NPC
- `Instance.SE_Instance` - это объект, который позволит нам хранить данные скрипта для каждого NPC отдельно
- `on_die.SE_FuncPtr` - функция обрабатывающая событие, когда персонаж умирает
- `CurrentState.SE_FuncPtr` - это текущая функция, которая обрабатывает поведение персонажа

Поскольку персонажей имеющих одинаковое поведение может быть много, то вероятно, в большинстве случаев они будут использовать один и тот же скрипт. Тоесть может быть множество объектов в которых логика реализована одинаково. В современных объектно-ориентированных языках программирования это решается с помощью **классов** и **объектов**. Класс описывает логику, а объект является экземпляром класса. В Skynet++ синтаксис ООП отсутствует, однако есть механизм инстансинга скриптов для хост-программы. Он использует объекты типа `SE_Instance` для хранения данных скрипта. Если проводить аналогии с ООП языками, то `SE_Script` - это **класс**, а `SE_Instance` - это **объект**. 

Что хранит в себе объект типа `SE_Instance`? Он хранит переменные которые имеют вид доступа `public`

Давайте рассмотрим как это работает на примере поведения персонажа. Для этого мы изменим функцию `UpdateNPC()`

```
Function UpdateNPC()
    Local NPCEntity = EntityCollided (Player, CT_NPC)

    For NPC.NPC = Each NPC
        If NPC\CurrentState <> Null Then
            SE_SetInstance(NPC\Instance)
            SE_CallUserFunction(NPC\CurrentState)
            SE_UnsetInstance(NPC\Instance)
        End If

        If NPC\MovementTime > 0 Then
            MoveEntity NPC\Entity, NPC\MovementX * NPC\MovementSpeed * TIME_DELTATIME, 0, NPC\MovementZ * NPC\MovementSpeed * TIME_DELTATIME
            NPC\MovementTime = NPC\MovementTime - TIME_DELTATIME
        End If

        If NPC\Entity = NPCEntity Then
            If NPC\IsEnemy = False Then
                If KeyHit(57) Then
                    If NPC\DialogId <> "" Then

                        SetDialog(NPC\DialogId)
                    End If
                End If
            Else
                CurrentEnemy = NPC

                Return
            End If

        End If
    Next
End Function
```

Мы добавили этот кусок кода:

```
SE_SetInstance(NPC\Instance)
SE_CallUserFunction(NPC\CurrentState)
SE_UnsetInstance(NPC\Instance)
```

Здесь происходит следующее:
1. Мы устанавливаем текущие объект для обработки NPC - **Instance**. Всем переменным имеющим вид доступа `public` присваиваются значения соответствующих переменных из объекта **Instance**
2. Выполняется обработка текущего состояния NPC
3. Производится сброс текущего объекта скрипта

Важно отметить, что всем переменным типа `public` не присваиваются значения по умолчанию. Для этого нужно выполнить функцию где им будет присвоено значение. Как это может выглядеть:

```
public gold_amount
public loot_drop

def on_after_spawn()
    gold_amount = 10;
    loot_drop = "Goblin Sword"
end
```
Внесём так же изменения в функцию `UpdateBattle()` и добавим вызов функции `on_die`:
```
    If CurrentEnemy\HP <= 0 Then
        HideEntity CurrentEnemy\Entity

        If CurrentEnemy\on_die <> Null Then
            SE_SetInstance(CurrentEnemy\Instance)
            CallMethod(CurrentEnemy\on_die, Handle(CurrentEnemy))
            SE_UnsetInstance(CurrentEnemy\Instance)
        End If

        CurrentEnemy = Null
    End If
```

Таким образом мы можем обработать одним скриптом множество одинаковых объектов.

Однако если сейчас мы запустим программу то ничего не произойдёт, т.к. мы ещё не загрузили скрипт

#### Загрузка скрипта

Для того чтобы загружаться скрипты для NPC изменим секцию `case "npc"` в функции `LoadMap`:

```
case "npc"
    PlaceholderName$ = NextToken()
    Local NPCName$ = NextToken()
    Script.SE_Script = LoadScript.SE_Script(NextToken())
    Local on_before_spawn.SE_FuncPtr = Null
    Local on_after_spawn.SE_FuncPtr = Null

    Local Spawn% = True

    If Script <> Null Then
        on_before_spawn.SE_FuncPtr = SE_FindFunc(Script, "on_before_spawn")
        on_after_spawn.SE_FuncPtr = SE_FindFunc(Script, "on_after_spawn")

        If on_before_spawn <> Null Then
            SE_CallUserFunction(on_before_spawn)
            Spawn = SE_ValueToInt(SE_RETURN_VALUE)
        End If
    End If

    If Spawn Then
        For Placeholder.MapPlaceholder = Each MapPlaceholder
            If Placeholder\Name = PlaceholderName$ Then
                Local NPC.NPC = CreateNPC(NPCName, Placeholder\x, Placeholder\z)
                NPC\HP = 100

                If Script <> Null Then
                    NPC\Script = Script
                    NPC\Instance = SE_CreateInstance(Script)
                    NPC\on_die = SE_FindFunc(Script, "on_die")
                    NPC\CurrentState = SE_FindFunc(Script, "state_idle")

                    If on_after_spawn <> Null Then
                        SE_SetInstance(NPC\Instance)
                        CallMethod(on_after_spawn, Handle(NPC))
                        SE_SetInstance(NPC\Instance)
                    End If
                End If
            EndIf
        Next
    End If
```

Мы не сохраняем функции ```on_before_spawn``` и ```on_after_spawn``` потому, что они используются единожды при загрузке, и для конкретного экземпляра **NPC** больше не понадобятся.

#### Подключение глобальных функций

Для реализации поведения NPC нам нужно написать и подключить глобальные функции, которые нам в этом помогут. Какие глобальные функции нам потребуются? В первую очередь мы должны иметь возможность как-то менять состояние NPC, т.е. переключать функцию, которая отвечает за обработку текущего поведения. Так же нам потребуются функции для перемещения персонажа в пространстве, а так же изменения его параметров, таких как здоровье, отношение к игроку и имя.

Добавим эти функции:

```
Function NPCSetState(NPCHandle, StateName$)
    Local NPC.NPC = Object.NPC(NPCHandle)

    If NPC = Null Then
        Return
    End If

    Local NewState.SE_FuncPtr = SE_FindFunc(NPC\Script, StateName)

    If NewState <> Null Then
        NPC\CurrentState = NewState
    End If
End Function

Function NPCMove(NPCHandle, X#, Z#)
    Local NPC.NPC = Object.NPC(NPCHandle)

    If NPC = Null Then
        Return
    End If

    SetNPCMovement(NPC, EntityX(NPC\Entity) + X, EntityZ(NPC\Entity) + Z)
End Function

Function NPCMoveTowardsPlayer(NPCHandle)
    Local NPC.NPC = Object.NPC(NPCHandle)

    If NPC = Null Then
        Return
    End If

    SetNPCMovement(NPC, EntityX(Player), EntityZ(Player))
End Function

Function NPCIsMoving(NPCHandle)
    Local NPC.NPC = Object.NPC(NPCHandle)

    If NPC = Null Then
        Return
    End If

    Return NPC\MovementTime > 0
End Function

Function NPCGetDistanceToPlayer(NPCHandle)
    Local NPC.NPC = Object.NPC(NPCHandle)

    If NPC = Null Then
        Return
    End If

    Return EntityDistance(NPC\Entity, Player)
End Function

Function NPCSetSpeed(NPCHandle, Speed#)
    Local NPC.NPC = Object.NPC(NPCHandle)

    If NPC = Null Then
        Return
    End If

    NPC\MovementSpeed = Speed#
End Function

Function NPCSetHP(NPCHandle, HP)
    Local NPC.NPC = Object.NPC(NPCHandle)

    If NPC = Null Then
        Return
    End If

    NPC\HP = HP
End Function

Function NPCGetHP#(NPCHandle)
    Local NPC.NPC = Object.NPC(NPCHandle)

    If NPC = Null Then
        Return
    End If

    Return NPC\HP
End Function

Function NPCSetAttackDamage(NPCHandle, AttackDamage)
    Local NPC.NPC = Object.NPC(NPCHandle)

    If NPC = Null Then
        Return
    End If

    NPC\AttackDamage = AttackDamage
End Function

Function NPCSetRelation(NPCHandle, IsEnemy)
    Local NPC.NPC = Object.NPC(NPCHandle)

    If NPC = Null Then
        Return
    End If

    NPC\IsEnemy = IsEnemy
End Function

Function NPCSetName(NPCHandle, Name$)
    Local NPC.NPC = Object.NPC(NPCHandle)

    If NPC = Null Then
        Return
    End If

    NPC\Name = Name
End Function

Function NPCSetDialogId(NPCHandle, DialogId$)
    Local NPC.NPC = Object.NPC(NPCHandle)

    If NPC = Null Then
        Return
    End If

    NPC\DialogId = DialogId
End Function

Function NPCFind%(Name$)
    For NPC.NPC = Each NPC
        If NPC\Name = Name Then
            Return Handle(NPC)
        End If
    Next
End Function
```

- NPCSetState(NPCHandle, StateName$) - будет менять текущую функцию поведения NPC по имени `"state_" + StateName`
- NPCMove(NPCHandle, X#, Z#) - устанавливает движение NPC от текущей позиции на определённое расстояние по осям X и Z
- NPCMoveTowardsPlayer(NPCHandle) - устанавливает движение NPC в сторону игрока
- NPCIsMoving(NPCHandle) - возвращает true если NPC ещё не закончил движение
- NPCSetSpeed(NPCHandle, Speed#) - устанавливает скорость передвижения NPC
- NPCSetHP(NPCHandle, HP) - Устанавливает здоровье NPC
- NPCGetHP%(NPCHandle) - Возвращает здоровье NPC
- NPCSetAttackDamage(NPCHandle, AttackDamage) - Устанавливает урон в бою от атаки NPC
- NPCSetRelation(NPCHandle, IsEnemy) - Меняет отношение NPC к игроку на враждебное или наоборот
- NPCSetName(NPCHandle, Name$) - Позволяет установить имя NPC
- NPCSetDialogId(NPCHandle, DialogId$) - Устанавливает диалог с NPC который начнется при взаимодействии
- NPCFind%(Name$) - Позволяет найти NPC по имени

Теперь когда мы разобрались для чего нужна каждая функция - подключим их в качестве глобальных. Вот так будет выглядеть ```SE_InvokeGlobalFunction```

```
Function SE_InvokeGlobalFunction(FunctionName$)
    If SE_BL_Math(FunctionName$) Then Return
    If SE_BL_Str(FunctionName$) Then Return
    If SE_BL_Array(FunctionName$) Then Return

    Select FunctionName
        Case "debug"
            DebugLog SE_ToStringArg(0, "")

        Case "check_item"
            SE_ReturnInt(CheckItem(SE_StringArg(0, "")))

        case "give_item"
            GiveItem(SE_ToStringArg(0, ""), SE_ToIntArg(1, 0))

        case "take_item"
            TakeItem(SE_ToStringArg(0, ""), SE_ToIntArg(1, 0))

        case "enough_items"
            SE_ReturnInt(EnoughItems(SE_ToStringArg(0, ""), SE_ToIntArg(1, 0)))

        case "set_door_state"
            SetDoorState(SE_ToIntArg(0, 0), SE_ToIntArg(1, 0))

        case "set_chest_state"
            SetChestState(SE_ToIntArg(0, 0), SE_ToIntArg(1, 0))

        case "npc_set_state"
            NPCSetState(SE_ToIntArg(0, 0), SE_ToStringArg(1, 0))

        case "npc_move"
            NPCMove(SE_ToIntArg(0, 0), SE_ToFloatArg(1, 0), SE_ToFloatArg(2, 0))

        case "npc_move_towards_player"
            NPCMoveTowardsPlayer(SE_ToIntArg(0, 0))

        case "npc_is_moving"
            SE_ReturnInt(NPCIsMoving(SE_ToIntArg(0, 0)))

        case "npc_get_distance_to_player"
            SE_ReturnFloat(NPCGetDistanceToPlayer(SE_ToIntArg(0, 0)))

        case "npc_set_speed"
            NPCSetSpeed(SE_ToIntArg(0, 0), SE_ToFloatArg(1, 0))

        case "npc_set_hp"
            NPCSetHP(SE_ToIntArg(0, 0), SE_ToFloatArg(1, 0))

        case "npc_get_hp"
            SE_ReturnFloat(NPCGetHP(SE_ToIntArg(0, 0)))

        case "npc_set_attack_damage"
            NPCSetAttackDamage(SE_ToIntArg(0, 0), SE_ToIntArg(1, 0))

        case "npc_set_relation"
            NPCSetRelation(SE_ToIntArg(0, 0), SE_ToIntArg(1, 0))

        case "npc_set_name"
            NPCSetName(SE_ToIntArg(0, 0), SE_ToStringArg(1, ""))

        case "npc_set_dialog_id"
            NPCSetDialogId(SE_ToIntArg(0, 0), SE_ToStringArg(1, ""))

        case "npc_find"
            SE_ReturnInt(NPCFind%(SE_ToStringArg(0, "")))
    End Select
End Function
```

#### Создаём скрипт

Давайте создадим базового врага в игре. Пусть это будет гоблин.

При создании NPC в игре мы теперь с помощью скрипта можем задать его параметры и описать его поведение с помощью функций.

Нам потребуется добавить врага на карту. Изменим для этого файл ```test_map.txt``` и добавим гоблина на месте плейсхолдера g. Файл ```test_map.txt``` будет теперь иметь вид:

```
**********************
*....................*
*....................*
*....................*
*.....g.......a.....@d
*....................*
*....................*
*....k........x......*
*....................*
*....................*
*.............c......*
*....................*
*....................*
**********************
entities

ground 0 255 0
door d 0 door.txt test_map2.txt
chest c chest.txt
chest k chest2.txt
npc x "Wise Old Man"
door a 0 door2.txt
npc g "Goblin" goblin.txt
```

Теперь создадим сам скрипт ```goblin.txt``` в папке `Scripts`:

```
def on_after_spawn(thisObject)
    npc_set_speed(thisObject, 2)
    npc_set_relation(thisObject, 1)
    npc_set_attack_damage(thisObject, 3)
end

def state_idle(thisObject)
    npc_move_towards_player(thisObject)
end
```

Теперь, когда мы запустим игру, у нас появится гоблин, который будет идти на игрока, пока не настигнет и после этого начнётся бой с ним.

Давайте немного усложним поведение гоблина и сделаем так чтобы он не сразу бежал к игроку, а только увидев его на определённом расстоянии. Пока он не видит игрока, он будет патрулировать локацию в случайных направлениях. Так же вынесем параметры NPC в отдельные константы, для лучшей читаемости кода:

```
const SPEED = 3
const IS_ENEMY = 1
const ATTACK_DAMAGE = 3

const MIN_PATROL_DISTANCE = 5
const MAX_PATROL_DISTANCE = 10
const MIN_ALERT_DISTANCE = 5

def on_after_spawn(thisObject)
    npc_set_speed(thisObject, SPEED)
    npc_set_relation(thisObject, IS_ENEMY)
    npc_set_attack_damage(thisObject, ATTACK_DAMAGE)
end

def state_idle(thisObject)
    npc_set_state(thisObject, "patrol")
end

def state_patrol(thisObject)
    if(not npc_is_moving(thisObject))
        local direction = math_rand(0, 4) * 90
        local distance = math_rnd(MIN_PATROL_DISTANCE, MAX_PATROL_DISTANCE)
        local x = math_cos(direction)*distance
        local y = math_sin(direction)*distance

        npc_move(thisObject, x, y)
    end

    local distance_to_player = npc_get_distance_to_player(thisObject)

    if(distance_to_player < MIN_ALERT_DISTANCE)
        npc_set_state(thisObject, "chase")
    end
end

def state_chase(thisObject)
    npc_move_towards_player(thisObject)
end
```

Теперь как только гоблин появляется на карте он начинает патрулировать карту в одном из четырёх направлений и завидев игрока - начинает его преследовать

При патрулировании (в пользовательской функции `state_patrol`) мы используем глобальную функцию `npc_is_moving()` для проверки закончил ли текущее передвижение NPC. Если он остановился - меняем направление движения на одно из четырёх (90, 180, 270 и 360 градусов) и движемся на расстояние которое определяется константами `MIN_PATROL_DISTANCE` и `MAX_PATROL_DISTANCE`. Обратите внимание, что здесь используются встроенные глобальные функции базовой библиотеки `math_...` описанные в файле `Skynet++ Base Lib.bb`. Далее с помощью функции `npc_get_distance_to_player` мы проверяем расстояние до игрока и если оно меньше `MIN_ALERT_DISTANCE` (т.е. видимое) то переходим в состояние погони `state_chase` где с помощью функции `npc_move_towards_player` NPC движется вслед за игроком. 

После того как мы победим гоблина - ничего не произойдёт. Давайте сделаем случайную награду для игрока после победы над гоблином. Для этого добавим функцию `reward`:

```
def reward()
    local items = ["Gold", "Gem", "Healing Potion"]
    local items_quantity = [100, 1, 1]

    local item_index = math_rand(0, 2)

    give_item(items[item_index], items_quantity[item_index])
end

def on_die()
    reward()
end
```

Готово. Теперь после победы над противником, мы будем получать один из трёх случайных предметов. Если мы зайдем на локацию, то гоблин снова нападёт на нас. Так можно фармить предметы бесконечно.

Давайте таким же образом создадим босса на локации test_map2.txt

```
**********************
*....................*
*...................b*
d@...................*
*....................*
**********************
entities

ground 50 50 50
door d 0 "" test_map.txt
npc b "Boss" boss.txt
```

Давайте просто скопируем скрипт гоблина. Сделаем босса немного сильнее повысив некоторые параметры и изменим награду за победу над ним:

```
const SPEED = 7
const IS_ENEMY = 1
const ATTACK_DAMAGE = 15

const MIN_PATROL_DISTANCE = 5
const MAX_PATROL_DISTANCE = 10
const MIN_ALERT_DISTANCE = 10

global defeated = false

def on_before_spawn(thisObject)
    return not defeated
end

def on_after_spawn(thisObject)
    npc_set_speed(thisObject, SPEED)
    npc_set_relation(thisObject, IS_ENEMY)
    npc_set_attack_damage(thisObject, ATTACK_DAMAGE)
end

def state_idle(thisObject)
    npc_set_state(thisObject, "patrol")
end

def state_patrol(thisObject)
    if(not npc_is_moving(thisObject))
        local direction = math_rand(0, 4) * 90
        local distance = math_rnd(MIN_PATROL_DISTANCE, MAX_PATROL_DISTANCE)
        local x = math_cos(direction)*distance
        local y = math_sin(direction)*distance

        npc_move(thisObject, x, y)
    end

    local distance_to_player = npc_get_distance_to_player(thisObject)

    if(distance_to_player < MIN_ALERT_DISTANCE)
        npc_set_state(thisObject, "chase")
    end
end

def state_chase(thisObject)
    npc_move_towards_player(thisObject)
end

def reward()
    give_item("Ring of Power", 1)
end

def on_die()
    defeated = true
    reward()
end
```

Готово. Теперь в соседней комнате мы можем победить Босса.

Большинство боссов в RPG проходятся единожды и не возрождаются после победы над ними. Для реализации этого мы используем метод `on_before_spawn` где проверяется значение глобальной переменной `defeated`. Переменная устанавливается как `true` при вызове функции `on_die`. В функии `reward` нам выдается только один предмет - "Ring of Power", который мы использьзуем в реализации примера в системе диалогов.

### Диалоги

В диалогах скрипты будут нам помогать в трех случаях:
1. Устанавливать старотовые диалоги для персонажей. Этот пункт уже выполнен в предыдущем разделе
2. Управлять доступностью вариантов ответов
3. Выполнять какие-то действия во время диалога. Например: дать предмет игроку

Где же нам внедрить скрипты? Прикреплять скрипт к диалогам было бы не очень удобно, потому что сам по себе диалог не отражает состояние чего-либо в игровом мире. Это просто заготовка сообщения для вывода на экран, а так же вариантов ответа на него. Что же описывает отдельную ситуацию в игре? Квесты! В нашем примере пока их нет. Однако поскольку у нас теперь есть скрипты, то мы можем реализовать квесты предельно просто - как отдельный скрипт и списком действий по квесту.

Напишем систему квестов сразу после системы диалогов

```
;Quests
Type Quest
    Field Id$
    Field Script.SE_Script
End Type

Function LoadQuest(FileName$)
    Local Script.SE_Script = LoadScript(FileName)

    If Script <> Null Then
        SE_CallUserFunction(Script\Main)

        Local Quest.Quest = New Quest
        Quest\Script = Script

        Local Id.SE_Public = SE_FindPublic(Script, "id")
        Quest\Id = SE_ValueToString(Id\Value)
    End If
End Function

Function QuestAction%(Id$, ActionName$)
    Local Result = 0

    For Quest.Quest = Each Quest
        If Quest\Id = FileName Then
            Local Func.SE_FuncPtr = SE_FindFunc(Quest\Script, ActionName)
            SE_CallUserFunction(Func)
            Result = SE_ValueToInt(SE_RETURN_VALUE)
        End If
    Next

    Return Result
End Function
```

В инициализации игры добавим загрузку квестов:

```
; Game initialization

SE_Init()
LoadQuest("quest.txt")
ReadDialogsFromFile("dialogs.txt")
LoadMap("test_map.txt")
```

Довольно немного. В функции `LoadQuest` мы загружаем скрипт квеста, затем выполняем его функцию _main и если у скрипта есть переменная типа `public` с именем `id` то квесту присваивается свой `id` по которому мы будем его искать. `QuestAction%(Id$, ActionName$)` позволяет найти квест и выполнить его действие вернув его результат в качестве целочисленного значения. Давайте подключим её для того чтобы можно было выполнять отдельные действия квеста из других скриптов.

Добавим в конец select-case SE_InvokeGlobalFunction следующую секцию case:

```
case "quest_action"
    SE_ReturnInt(QuestAction(SE_ToStringArg(0, ""), SE_ToStringArg(1, "")))
```

Готово. Теперь мы можем использовать эту функцию в скриптах.

Напишем сначала скрипт квеста:

```
public id = "first_quest"

global boss_defeated = false

def defeat_boss()
    boss_defeated = true
end

def completed()
    return boss_defeated
end
```

В нём нам доступны две функции. Первая позволяет установить флаг победы над боссом, а вторая - проверить выполнен ли квест. Давайте изменим функции `on_die` для босса:

```
def on_die()
    defeated = true
    reward()
    quest_action("first_quest", "defeat_boss")
end
```

Теперь допишем диалог, чтобы после победы над Боссом персонаж уже не говорил нам что рядом монстры:

```
#dialog start
Be careful!
Monsters roaming here

#option what_they_looking_for
What they looking for?

#option how_many
How many monsters here?

#dialog what_they_looking_for
I don't know.
I hid here when they came in
Help me. Defeat the monsters, and I'll give you reward!

#dialog how_many
At least two
One here and a bigger one behind the door


#dialog safe_now
Thank you! Here is safe now

#option reward
How about reward?

#option ring
What do you know about this ring?

#dialog reward
This healing potion can help you

#dialog ring
This is the Ring of Power
```

Для того чтобы персонаж менял диалог нам нужно создать скрипт для персонажа wise_old_man.txt:

```
def on_after_spawn(thisObject)
    if(quest_action("first_quest", "completed"))
        npc_set_dialog_id(thisObject, "safe_now")
    end
end
```

Не забываем отредактировать test_map.txt:

```
npc x "Wise Old Man" wise_old_man.txt
```

Теперь когда мы возвращаемся к персонажу Wise Old Man после победы над Боссом - его диалог меняется на `safe_now`.

Однако, этот персонаж, если судить по диалогу в секции `#dialog reward`, должен нас наградить. На данный момент ничего не происходит. Давайте исправим это. Рассмотрим добавленную нами часть диалога:

```
#dialog safe_now
Thank you! Here is safe now

#option reward
How about reward?

#option ring
What do you know about this ring?

#dialog reward
This healing potion can help you

#dialog ring
This is the Ring of Power
```

На строке...

```
This healing potion can help you
```

...Должно происходить вознаграждение. Мы можем добавить выполнение действий к ней. Для этого нам потребуется расширить тип `DialogMessage`:

```
Type DialogMessage
    Field Dialog.Dialog
    Field Message$
    Field MessageKey$
    Field NextMessage.DialogMessage
    Field OptionsCount%
    Field Action.SE_FuncPtr
End Type
```

Здесь мы добавили `Field Action.SE_FuncPtr`

Вывод текущего сообщения диалога происходит в функции `SetDialogMessage`. В неё мы и добавим вызов действия при сообщении:

```
Function SetDialogMessage(DialogMessage.DialogMessage)
    If DialogMessage <> Null Then
        CurrentDialogMessage = DialogMessage
        SetDialogOptions()

        If DialogMessage\Action <> Null Then
            SE_CallUserFunction(DialogMessage\Action)
        End If
    Else
        EndDialog()
    End If
End Function
```

Теперь давайте определим как мы будем указывать скрипт в диалоге. У нас уже есть секции начинающиеся на `#`. Мы можем добавить новую секцию, например `#action quest_id quest_action` где будем указывать действие `quest_action` вызываемое из скрипта квеста `quest_id`. 
Диалог с вознаграждением будет выглядеть вот так:

```
#dialog reward
This healing potion can help you
#action first_quest reward
```

Так же добавим в скрипте квеста само действие вознаграждения:

```
def reward()
    give_item("Healing Potion", 1)
end
```

Чтобы это действие назначалось сообщению нужно доработать загрузку диалогов:

```
Function ReadDialogsFromFile(FileName$)
    Local File = ReadFile(FileName$)

    While Not Eof(File)
        Local LineText$ = Trim(ReadLine(File))

        ParseString(LineText)

        Local TokenText$ = NextToken()

        If TokenText = "#dialog" Then
            Local D.Dialog = CreateDialog(NextToken())
            DebugLog D\Id

        Else If TokenText = "#option" Then
            Local NextDialogId$ = NextToken()
            Local OptionText$ = "[EOF]"

            If Not Eof(File) Then
                OptionText$ = Trim(ReadLine(File))
            End If

            Local Option.DialogOption = CreateDialogOption(OptionText$, NextDialogId$)

        Else If TokenText = "#action"
            If CurrentDialogMessage <> Null Then
                Local QuestId$ = NextToken()
                Local ActionName$ = NextToken()

                Local Quest.Quest = FindQuest(QuestId, ActionName)

                If Quest <> Null Then
                    CurrentDialogMessage\Action = SE_FindFunc(Quest\Script, ActionName)
                End If
            End If

        Else If LineText <> "" Then
            CreateDialogMessage(LineText)
        End If
    Wend

    EndDialog()
End Function
```

Всё. Теперь если запустить игру, то после победы над боссом, вернуться к NPC `Wise Old Man` то мы можем получить награду. Вроде бы всё работает, но если повторить диалог, то мы снова получим награду. Чтобы решить эту проблему нам потребуется добавить условие доступности варианта ответа. Мы будем выводить вариант ответа только если выполняется некое необходимое условие.

Добавим в `#option` дополнительные поля `quest_id` и `condition`:

`#option dialog_id quest_id condition`

Расширим тип `DialogOption`

```
Type DialogOption
    Field Dialog.Dialog
    Field Message.DialogMessage
    Field OptionText$
    Field NextDialogId$
    Field Available
    Field Condition.SE_FuncPtr
End Type
```

Здесь мы добавили `Field Condition.SE_FuncPtr` - функцию при вызове которой будет проверяться доступность варианта ответа.

Вывод диалогов у нас производится в функции `SetDialogOptions`. Отредактируем эту функцию:

```
Function SetDialogOptions()
    Dim CurrentDialogOptions.DialogOption(CurrentDialogMessage\OptionsCount)

    CurrentDialogOption = 0

    Local OptionIndex = 0

    For DialogOption.DialogOption = Each DialogOption
        If DialogOption\Dialog = CurrentDialog And DialogOption\Message = CurrentDialogMessage Then
            DialogOption\Available = True

            If DialogOption\Condition <> Null Then
                SE_CallUserFunction(DialogOption\Condition)
                DialogOption\Available = SE_ValueToInt(SE_RETURN_VALUE)
            End If

            CurrentDialogOptions(OptionIndex) = DialogOption;
            OptionIndex = OptionIndex + 1
        End If
    Next
End Function
```

Модифицируем загрузку секции `#option` в `ReadDialogsFromFile`:

```
        Else If TokenText = "#option" Then
            Local NextDialogId$ = NextToken()
            Local OptionText$ = "[EOF]"

            If Not Eof(File) Then
                OptionText$ = Trim(ReadLine(File))
            End If

            Local Option.DialogOption = CreateDialogOption(OptionText$, NextDialogId$)

    EndDialog()
End Function
```

Теперь допишем диалог:

```
#dialog safe_now
Thank you! Here is safe now

#option reward first_quest not_rewarded
How about reward?

#option ring
What do you know about this ring?

#dialog reward
This healing potion can help you
#action first_quest reward

#dialog ring
This is the Ring of Power
```

...И скрипт:

```
public id = "first_quest"

global boss_defeated = false
global rewarded = false

def defeat_boss()
    boss_defeated = true
end

def completed()
    return boss_defeated
end

def reward()
    give_item("Healing Potion", 1)
    rewarded = true
end

def not_rewarded()
    return not rewarded
end
```

Давайте запустим игру и посмотрим, как всё работает.

Теперь персонаж может наградить нас только один раз, после победы над Боссом. Остался последний штрих - сделаем так чтобы после победы над боссом, гоблин не возрождался. Для этого добавим в его скрипте функцию:

```
def on_before_spawn(thisObject)
    return not quest_action("first_quest", "completed")
end
```

# Итог

Вот и всё.

Хотя данный пример не представляет собой эталон игровой логики, но всё же его основной смысл - показать основные принципы внедрения Skynet++ для программ на BlitzBasic. Надеюсь, с этой задачей он справился :)

Как итог - мы получили более гибкую настройку взаимодействия сущностей игры через скрипты

В заключение хочется отметить основные рекомендации, которых стоит придерживаться при внедрении Skynet++ в проект:
- Скрипты предназначены в первую очередь для реализации более высокоуровневой логики. Весь  низкоуровневый функционал необходимо подключать с помощью глобальных функций, т.к. это напрямую влияет на производительность
- Перед внедрением скриптов для отдельной сущности, сначала как следует проанализируйте возможные события для него, а так же то, какие глобальные функции будут ею использоваться
- Для обработки множества однотипных объектов, лучше использовать инстансинг, где это возможно

