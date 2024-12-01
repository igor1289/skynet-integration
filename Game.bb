Include "Skynet++.bb"

Graphics3D 1024, 768, 32, 2
SetBuffer BackBuffer()

SeedRnd Millisecs()

SetFont LoadFont("Arial",14,False,False,False)

;Misc
Global TIME_DELTATIME# = 0, TIME_MILLISECS = MilliSecs ()

Function UpdateDeltaTime()
    Local ms = MilliSecs ()
    TIME_DELTATIME = (ms - TIME_MILLISECS) / 1000.0
    TIME_MILLISECS = ms
End Function

Function DrawText(TextString$, X, Y, R=255, G=255, B=255)
    Color 0, 0, 0
    Rect X, Y, StringWidth(TextString), StringHeight(TextString)
    Color R, G, B
    Text X, Y, TextString
End Function

Type Token
    Field String$
End Type

Global CurrentToken.Token

Function ParseString(SrcString$)
    Delete Each Token

    Local Length = Len(SrcString)

    Local Token.Token

    Local ExpectingDoubleQuote = False

    For Offset = 1 To Length
        Local Char$ = Mid(SrcString, Offset, 1)
        Local Code = Asc(Char)

        If Code >= 33 And Code <= 126 Then
            If Token = Null Then Token = New Token

            If Code <> 34 Then
                Token\String = Token\String + Char
            Else
                If ExpectingDoubleQuote Then Token = Null
                ExpectingDoubleQuote = Not ExpectingDoubleQuote
            End If
        Else
            If ExpectingDoubleQuote = False Then
                Token = Null
            Else
                Token\String = Token\String + Char
            End If
        End If
    Next
End Function

Function NextToken$()
    If CurrentToken <> Null Then
        CurrentToken = After CurrentToken
    Else
        CurrentToken = First Token
    End If

    If CurrentToken <> Null Then
        Return CurrentToken\String
    Else
        Return ""
    End If
End Function

Function NextTokens()
    Local Result$ = ""

    Repeat
        Local String$ = NextToken()
        Result$ = Result$ + " " + String

    Until String$ = ""

    Return Trim(Result)
End Function

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

Function CallMethod(Method.SE_FuncPtr, ObjectHandle = 0)
    If Method = Null Then
        Return
    End If

    If ObjectHandle <> 0 Then
        SE_AddIntArg(ObjectHandle)
    End If

    SE_CallUserFunction(Method)
End Function

;Function DumpArguments()
;    Local VT$[10]
;    VT[SE_NULL] = "Null"
;    VT[SE_INT] = "Int"
;    VT[SE_FLOAT] = "Int"
;    VT[SE_STRING] = "Int"
;
;
;    DebugLog "======================"
;    DebugLog "ARGUMENTS COUNT: "+ SE_ARGUMENTS_NUMBER
;    For I=0 To SE_ARGUMENTS_NUMBER - 1
;        DebugLog VT[SE_ArgType(I)] + "=" + SE_ToStringArg(I)
;    Next
;    DebugLog "======================"
;End Function


;Main Camera
Global Camera = CreateCamera()
PositionEntity Camera, 0, 25, 0
TurnEntity Camera, 90, 0, 0

;Light
Global Light = CreateLight()

;Player
Const CT_Player = 1

Global PlayerSize# = .5
Global PlayerSpeed# = 10;

Global PlayerHP# = 100
Global PlayerAttackDamage# = 25

Global Player = CreateSphere()
ScaleEntity Player, PlayerSize, PlayerSize, PlayerSize
EntityRadius Player, PlayerSize, PlayerSize
EntityColor Player, 255, 255, 0
EntityType Player, CT_Player
EntityParent Camera, Player

Function PlayerMovement()
    Local DirX#, DirZ#

    If KeyDown(30) Then DirX = DirX - 1
    If KeyDown(32) Then DirX = DirX + 1
    If KeyDown(31) Then DirY = DirY - 1
    If KeyDown(17) Then DirY = DirY + 1

    MoveEntity Player, DirX * PlayerSpeed * TIME_DELTATIME, 0, DirY * PlayerSpeed * TIME_DELTATIME
End Function

;Map
Const CT_Wall = 2
Global MapPlane = CreatePlane()

Global MapStartX# = 0, MapStartZ# = 0
Global MapOffsetX# = MapStartX#, MapOffsetZ# = MapStartZ#
Global MapStep# = 2

Type MapPlaceholder
    Field Name$
    Field x#, z#
End Type

Function MapRow(Row$)
    Local Length = Len(Row)

    For Offset = 1 To Length
        Local Char$ = Trim(Mid(Row, Offset, 1))

        If Char <> "" Then
            Select Char
                Case "*"
                    Local Wall = CreateCube(MapPlane)
                    EntityType Wall, CT_Wall
                    PositionEntity Wall, MapOffsetX, 0, MapOffsetZ

                Case "@"
                    HideEntity Player
                    PositionEntity Player, MapOffsetX, 0, MapOffsetZ
                    ShowEntity Player

                Default
                    If Char<>"." Then
                        Local Placeholder.MapPlaceholder = New MapPlaceholder
                        Placeholder\Name = Char
                        Placeholder\x = MapOffsetX
                        Placeholder\z = MapOffsetZ
                    End If

            End Select
            MapOffsetX = MapOffsetX + MapStep
        EndIf
    Next

    MapOffsetZ = MapOffsetZ - MapStep;
    MapOffsetX = MapStartX
End Function

Function FindPlaceholder.MapPlaceholder(Name$)
    For Placeholder.MapPlaceholder = Each MapPlaceholder
        If Placeholder\Name = Name Then
            Return Placeholder
        EndIf
    Next
End Function

Function LoadMap(FileName$)
    ClearDoors()
    ClearChests()
    ClearNPCs()

    Delete Each MapPlaceholder

    FreeEntity MapPlane
    ; MapOffsetX# = MapStartX#
    ; MapOffsetZ# = MapStartZ#

    Local File = ReadFile(FileName)

    MapPlane = CreatePlane()
    EntityColor MapPlane, 0, 255, 0

    While Not Eof(File)
        Local RowText$ = Trim(ReadLine(File))

        If RowText$ = "entities" Then
            Exit
        End If

        MapRow(RowText$)
    Wend

    While Not Eof(File)
        Local LineText$ = Trim(ReadLine(File))

        ParseString(LineText)

        Local Command$ = NextToken()
        Local PlaceholderName$ = ""

        Select Command
            Case "ground"
                Local GroundR% = Int(NextToken())
                Local GroundG% = Int(NextToken())
                Local GroundB% = Int(NextToken())
                EntityColor MapPlane, GroundR, GroundG, GroundB

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
                            NPC\DialogId = "start"

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
        End Select
    Wend
End Function

;Doors
Const CT_Door = 3

Type Door
    Field Entity
    Field Opened
    Field Map$

    Field on_before_open.SE_FuncPtr
    Field on_after_open.SE_FuncPtr

    Field on_create.SE_FuncPtr
    Field on_update.SE_FuncPtr
End Type

Function CreateDoor.Door(X#, Z#, Opened, Map$ = "", R=50, G=50, B=100)
    Local Door.Door = New Door
    Door\Entity = CreateCube()
    EntityType Door\Entity, CT_Door
    EntityColor Door\Entity, R, G, B
    PositionEntity Door\Entity, X, 0, Z

    SetDoorOpened(Door, Opened)

    Door\Map = Map

    Return Door
End Function

Function SetDoorOpened(Door.Door, Opened)
    If Opened = True Then
        If Door\Map$ <> "" Then
            DebugLog Door\Map$
            LoadMap(Door\Map$)
            Return
        Else
            HideEntity Door\Entity

            If Door\on_after_open <> Null Then
                SE_CallUserFunction(Door\on_after_open)
            End If
        End If
    Else
        ShowEntity Door\Entity
    End If

    Door\Opened = Opened
End Function

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

Function ClearDoors()
    For Door.Door = Each Door
        FreeEntity Door\Entity
    Next

    Delete Each Door
End Function

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

;NPC
Const CT_NPC = 4

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


Function CreateNPC.NPC(Name$, X#, Z#, Size#=1, R=100, G=100, B=100, Alpha# = 1, Shine#=0)
    Local NPC.NPC = New NPC
    NPC\Name = Name
    NPC\Entity = CreateSphere()
    PositionEntity NPC\Entity, X, 0, Z
    ScaleEntity NPC\Entity, Size, Size, Size
    EntityColor NPC\Entity, R, G, B
    EntityAlpha NPC\Entity, Alpha
    EntityShininess NPC\Entity, Shine
    EntityType NPC\Entity, CT_NPC

    Return NPC
End Function

Function SetNPCMovement(NPC.NPC, X#, Z#)
    Local DirX# = X - EntityX(NPC\Entity)
    Local DirZ# = Z - EntityZ(NPC\Entity)

    Local A# = ATan2(DirZ, DirX)
    NPC\MovementX = Cos(A)
    NPC\MovementZ = Sin(A)
    NPC\MovementTime = Sqr(DirX^2 + DirZ^2) / NPC\MovementSpeed
End Function

Function UpdateNPC()
    Local NPCEntity = EntityCollided (Player, CT_NPC)

    For NPC.NPC = Each NPC
        If NPC\CurrentState <> Null Then
            SE_SetInstance(NPC\Instance)
            CallMethod(NPC\CurrentState,  Handle(NPC))
            SE_UnsetInstance(NPC\Instance)
        End If

        If NPC\MovementTime > 0 Then
            MoveEntity NPC\Entity, NPC\MovementX * NPC\MovementSpeed * TIME_DELTATIME, 0, NPC\MovementZ * NPC\MovementSpeed * TIME_DELTATIME
            NPC\MovementTime = NPC\MovementTime - TIME_DELTATIME
        End If

        If NPC\Entity = NPCEntity Then
            NPC\MovementTime = 0

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


Function ShowNPCNames()
    For NPC.NPC = Each NPC
        If NPC\HP > 0 Then
            CameraProject Camera, EntityX(NPC\Entity), EntityY(NPC\Entity), EntityZ(NPC\Entity)

            If NPC\IsEnemy Then
                Color 255, 0, 0
                DrawText(NPC\Name + " [HP:" + Int(NPC\HP) +"] ", ProjectedX(), ProjectedY(), 255, 0, 0)
            Else
                DrawText(NPC\Name + " [HP:" + Int(NPC\HP) +"]", ProjectedX(), ProjectedY())
            End If


        End If
    Next
End Function

Function ClearNPCs()
    For NPC.NPC = Each NPC
        FreeEntity NPC\Entity
    Next

    Delete Each NPC
End Function

Function NPCSetState(NPCHandle, StateName$)
    Local NPC.NPC = Object.NPC(NPCHandle)

    If NPC = Null Then
        Return
    End If

    Local NewState.SE_FuncPtr = SE_FindFunc(NPC\Script, "state_" + StateName)

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

;Chests
Const CT_Chest = 5

Type Chest
    Field Entity, Top
    Field ItemName$, ItemCount%
    Field Opened

    Field on_before_open.SE_FuncPtr
    Field on_after_open.SE_FuncPtr

    Field on_create.SE_FuncPtr
    Field on_update.SE_FuncPtr
End Type

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
        End If
    End If

    SetChestOpened(Chest)
End Function

Function SetChestOpened(Chest.Chest)
    If Chest\Opened Then
        EntityColor Chest\Top, 0, 0, 0
    Else
        EntityColor Chest\Top, 150, 150, 150
    End If
End Function

Function UpdateChests()
    Local ChestEntity = EntityCollided ( Player, CT_Chest )

    For Chest.Chest = Each Chest
        If Chest\Entity = ChestEntity Then
            If KeyHit(57) Then
                OpenChest(Chest)
            End If
        End If
    Next
End Function

Function ClearChests()
    For Chest.Chest = Each Chest
        FreeEntity Chest\Entity
    Next

    Delete Each Chest
End Function

Function SetChestState(ChestHandle, Opened)
    Local Chest.Chest = Object.Chest(ChestHandle)

    If Chest <> Null Then
        Chest\Opened = Opened
        SetChestOpened(Chest)
    End If
End Function

;Items
Type Item
    Field Name$
    Field Count

    Field BattleUsage
    Field DamageOnUse#
    Field HealOnUse#

    Field AttackBuff#
    Field DefenceBuff#
End Type

Function AddItem.Item(Name$, Count)
    For Item.Item = Each Item
        If Item\Name = Name$ Then
            Item\Count = Item\Count + Count
            Return
        End If
    Next

    Item = New Item
    Item\Name = Name
    Item\Count = Count

    return Item
End Function

Function ShowItems()
    Color 255, 255, 255

    Local ItemsOffset, LineHeight = 20

    For Item.Item = Each Item
        If Item\Count > 0 Then
            DrawText(Item\Name + " x " + Item\Count, 0, ItemsOffset)
            ItemsOffset = ItemsOffset + LineHeight
        End If
    Next
End Function

Function CheckItem(Name$)
    For Item.Item = Each Item
        If Item\Name = Name And Item\Count > 0 Then
            Return True
        End If
    Next
End Function

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

Function TakeItem(Name$, Amount)
    For Item.Item = Each Item
        If Item\Name = Name Then
            Item\Count = Item\Count - Amount
            If Item\Count <= 0 Then Delete Item;
            Exit
        End If
    Next
End Function

Function EnoughItems(Name$, Amount)
    For Item.Item = Each Item
        If Item\Name = Name Then
            Return Item\Count >= Amount
        End If
    Next

    Return False
End Function



;Dialog
Global CurrentDialog.Dialog
Global CurrentDialogMessage.DialogMessage

Dim CurrentDialogOptions.DialogOption(0)

Global CurrentDialogOption = 0

Type Dialog
    Field Id$
    Field FirstMessage.DialogMessage
End Type

Type DialogMessage
    Field Dialog.Dialog
    Field Message$
    Field MessageKey$
    Field NextMessage.DialogMessage
    Field OptionsCount%
    Field Action.SE_FuncPtr
End Type

Type DialogOption
    Field Dialog.Dialog
    Field Message.DialogMessage
    Field OptionText$
    Field NextDialogId$
    Field Available
    Field Condition.SE_FuncPtr
End Type

Function CreateDialog.Dialog(Id$)
    Local Dialog.Dialog = New Dialog
    Dialog\Id = Id

    CurrentDialog = Dialog;
    CurrentDialogMessage = Null

    Return Dialog
End Function

Function CreateDialogMessage.DialogMessage(Message$)
    Local DialogMessage.DialogMessage = New DialogMessage
    DialogMessage\Dialog = CurrentDialog
    DialogMessage\Message$ = Message$

    If CurrentDialogMessage = Null Then
        CurrentDialog\FirstMessage = DialogMessage
    Else
        CurrentDialogMessage\NextMessage = DialogMessage
    End If

    CurrentDialogMessage = DialogMessage

    Return DialogMessage
End Function

Function CreateDialogOption.DialogOption(OptionText$, NextDialogId$)
    Local DialogOption.DialogOption = New DialogOption
    DialogOption\Dialog = CurrentDialog
    DialogOption\Message = CurrentDialogMessage
    DialogOption\OptionText$ = OptionText$
    DialogOption\NextDialogId$ = NextDialogId$

    CurrentDialogMessage\OptionsCount = CurrentDialogMessage\OptionsCount + 1

    Return DialogOption
End Function

Function SetDialog(Id$)
    For Dialog.Dialog = Each Dialog
        If Dialog\Id = Id Then
            CurrentDialog = Dialog
            SetDialogMessage(CurrentDialog\FirstMessage)
            SetDialogOptions()
            Exit
        End If
    Next
End Function

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

Function EndDialog()
    CurrentDialog = Null
    CurrentDialogMessage = Null
    Dim CurrentDialogOptions.DialogOption(0)
End Function

Function RenderDialog()
    Local LineHeight = 16
    DrawText(CurrentDialogMessage\Message, 0, 0)

    Local OptionOffset = 0

    For OptionIndex = 0 To CurrentDialogMessage\OptionsCount - 1
        If CurrentDialogOptions(OptionIndex)\Available Then
            If OptionIndex <> CurrentDialogOption Then
                DrawText(CurrentDialogOptions(OptionIndex)\OptionText, 0, LineHeight + OptionOffset * LineHeight)
            Else
                DrawText("->" + CurrentDialogOptions(OptionIndex)\OptionText, 0, LineHeight + OptionOffset * LineHeight)
            EndIf

            OptionOffset = OptionOffset + 1
        End If
    Next
End Function

Function UpdateDialog()
    If CurrentDialogMessage\OptionsCount > 0 Then
        If KeyHit(17) And CurrentDialogOption > 0 Then
            CurrentDialogOption = CurrentDialogOption - 1
        EndIf

        If KeyHit(31) And CurrentDialogOption < CurrentDialogMessage\OptionsCount - 1 Then
            CurrentDialogOption = CurrentDialogOption + 1
        EndIf

        If KeyHit(57) Then
            SetDialog(CurrentDialogOptions(CurrentDialogOption)\NextDialogId)
        End If
    Else
        If KeyHit(57) Then
            SetDialogMessage(CurrentDialogMessage\NextMessage)
        End If
    End If
End Function

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
            Local OptionQuestId$ = NextToken()
            Local OptionActionName$ = NextToken()
            Local OptionText$ = "[EOF]"

            If Not Eof(File) Then
                OptionText$ = Trim(ReadLine(File))
            End If

            Local Option.DialogOption = CreateDialogOption(OptionText$, NextDialogId$)

            Local OptionQuest.Quest = FindQuest(OptionQuestId, OptionActionName)

            If OptionQuest <> Null Then
                Option\Condition = SE_FindFunc(OptionQuest\Script, OptionActionName)
            End If

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

Function FindQuest.Quest(Id$, ActionName$)
    Local Result = 0

    For Quest.Quest = Each Quest
        If Quest\Id = Id Then
            Return Quest
        End If
    Next
End Function

Function QuestAction%(Id$, ActionName$)
    Local Result = 0

    Local Quest.Quest = FindQuest.Quest(Id$, ActionName$)

    If Quest <> Null Then
        Local Func.SE_FuncPtr = SE_FindFunc(Quest\Script, ActionName)
        SE_CallUserFunction(Func)
        Result = SE_ValueToInt(SE_RETURN_VALUE)
    End If

    Return Result
End Function

;Battle
Global CurrentEnemy.NPC
Global EnemyTurn, EnemyWaiting#
Global SelectedAction

Function UpdateBattle()
    Local AttackBuff# = 0
    Local DefenceBuff# = 0

    For Item.Item = Each Item
        AttackBuff = AttackBuff + Item\AttackBuff
        DefenceBuff = DefenceBuff + Item\DefenceBuff
    Next

    If EnemyTurn Then
        If EnemyWaiting > 0 Then
            EnemyWaiting = EnemyWaiting - TIME_DELTATIME
        Else
            Local TotalDamage# = CurrentEnemy\AttackDamage - DefenceBuff

            If TotalDamage > 0 Then
                PlayerHP = PlayerHP - TotalDamage
            End If

            EnemyTurn = False
        End If
    Else
        Local CombatItemsCount = 0

        For Item.Item = Each Item
            If Item\BattleUsage And Item\Count > 0 Then
                CombatItemsCount = CombatItemsCount + 1
            EndIf
        Next

        If SelectedAction > CombatItemsCount Then
            SelectedAction = 0
        End If

        If KeyHit(17) And SelectedAction > 0 Then
            SelectedAction = SelectedAction - 1
        End If

        If KeyHit(31) And SelectedAction < CombatItemsCount Then
            SelectedAction = SelectedAction + 1
        End If

        If KeyHit(57) then
            If SelectedAction = 0 Then
                TotalDamage# = PlayerAttackDamage + AttackBuff
                CurrentEnemy\HP = CurrentEnemy\HP - TotalDamage
            Else
                Local ItemNumber = 0
                Local SeletedItem.Item

                For Item.Item = Each Item
                    If Item\BattleUsage And Item\Count > 0 Then
                        ItemNumber = ItemNumber + 1

                        If ItemNumber = SelectedAction Then
                            SeletedItem = Item
                            Exit
                        End If
                    EndIf
                Next

                If SeletedItem\HealOnUse > 0 Then
                    PlayerHP = PlayerHP + SeletedItem\HealOnUse
                EndIf

                If SeletedItem\DamageOnUse > 0 Then
                    CurrentEnemy\HP = CurrentEnemy\HP - SeletedItem\DamageOnUse
                EndIf

                SeletedItem\Count = SeletedItem\Count - 1
            EndIf

            EnemyTurn = True
            EnemyWaiting = 1
        Endif
    End If

    If CurrentEnemy\HP <= 0 Then
        HideEntity CurrentEnemy\Entity

        If CurrentEnemy\on_die <> Null Then
            SE_SetInstance(CurrentEnemy\Instance)
            CallMethod(CurrentEnemy\on_die, Handle(CurrentEnemy))
            SE_UnsetInstance(CurrentEnemy\Instance)
        End If

        CurrentEnemy = Null
    End If
End Function

Function ShowBattleHUD()
    Local LineHeight = 20

    DrawText("Player HP: " + PlayerHP, 0, 0)
    DrawText("Enemy HP: " + CurrentEnemy\HP, 0, LineHeight)
    DrawText("------Actions------", 0, LineHeight * 2)

    If SelectedAction = 0 Then
        DrawText("Attack <-", 0, LineHeight * 3)
    Else
        DrawText("Attack", 0, LineHeight * 3)
    EndIf

    Local ItemsOffset = LineHeight * 3
    Local ItemNumber = 0

    For Item.Item = Each Item
        If Item\BattleUsage And Item\Count > 0 Then
            ItemNumber = ItemNumber + 1

            If ItemNumber = SelectedAction Then
                DrawText("Use " + Item\Name + "<-", 0, ItemsOffset + ItemNumber * LineHeight)
            Else
                DrawText("Use " + Item\Name, 0, ItemsOffset + ItemNumber * LineHeight)
            End If
        EndIf
    Next

End Function


;Game
Function GameUpdate()
    If CurrentDialog <> Null Then
        UpdateDialog()
    Else If CurrentEnemy <> Null
        UpdateBattle()
    Else
        PlayerMovement()
        UpdateDoors()
        UpdateChests()
        UpdateNPC()
    End If

    If PlayerHP < 0 Then
        RuntimeError "Game Over!"
    End If
End Function

Function GameHUD()
    If CurrentDialog <> Null Then
        RenderDialog()

    Else If CurrentEnemy <> Null
        ShowBattleHUD()
    Else
        ShowItems()
    End If

    ShowNPCNames()
End Function





; Game initialization

SE_Init()
LoadQuest("quest.txt")
ReadDialogsFromFile("dialogs.txt")
LoadMap("test_map.txt")

;Set up collisions
Collisions CT_Player, CT_Wall, 3, 2
Collisions CT_Player, CT_Door, 3, 2
Collisions CT_Player, CT_NPC, 1, 2
Collisions CT_NPC, CT_Player , 1, 2
Collisions CT_NPC, CT_Wall, 3, 2
Collisions CT_Player, CT_Chest, 3, 2

Repeat
    UpdateDeltaTime()
    GameUpdate()

    UpdateWorld
    RenderWorld

    GameHUD()
    Flip
Until KeyHit(1)
End