;
;	Sample BIN ROM image for creating sample ".ROM" and ".CAR" files.
;	System equates are defined in the main program depending on the Atari architecture.
;	Compile using "Makefile.bat".
;	Note that for types with very small banks not all texts is displayed.
;	Example: "AST 32 KB (47)" with 256 byte banks
;
;	(c) 2013-06-07 JAC!	
;	(r) 2018-05-30 JAC!	
;
;	@com.wudsn.ide.lng.mainsourcefile=CartridgeTypeSampleCreator-Atari800.asm

p1	= $80
p2	= $82

	opt h-f+

	org $0600
	.proc main			;Assume CLD is set

	.byte $ff,$ff,$00,$00,$00,$00	;Maxflash Cartridge Studio fix
start_offset
	lda #>main			;High byte of PC, updated by external program at startOffset+1
	.byte $2c,a(ram.text-main)	;BIT abs to store text offset word which is read by external program at startOffset+3/4
	sta p1+1
	lda #>main
	sta p2+1
	ldy #0
	sty antic+$0e			;NMIEN
	sty p1
	sty p2
	ldx #>[.len main + $ff]
copy_loop
	mva (p1),y (p2),y
	iny
	bne copy_loop
	inc p1+1
	inc p2+1
	dex
	bne copy_loop
	jmp ram
	
	.proc ram
	mwa #xitvbv vbiv
	mva #$40 antic+$0e		;NMIEND
	mva #14 gtia+$17		;COLPF1, white text foreground
	mwa #dl antic+$02		;DLISTL/H
	mva #$22 antic+$00		;DMCTL
	mva #>font antic+$09		;CHBASE

	ldx #[.len text]
text_loop
	lda text-1,x			;Convert ASCII to screen code
	cmp #96
	scs
	sbc #31
	sta text-1,x
	dex
	bne text_loop
	stx gtia+$18			;Black text background

forever_loop
	mva antic+$0b gtia+$1a
	jmp forever_loop

	.local dl
:9	.byte $70
	.rept 5
	.byte $42,a(text+#*40)		;Text line
	.if # < 4
	.byte $48,a(line)		;Black line
	.endif
	.endr
	.byte $41,a(dl)
	.endl

	.local text			;6 lines of text, kept in ASCII in the ROM, so it can be read in the hex editor
:240	.byte 0
	.endl

	.local line
	.byte $55,$55,$55,$55,$55,$55,$55,$55,$55,$55	;COLPF0, black
	.endl

	.endp				;End of ram
  	
  	.endp				;End of main

  	.print .len main