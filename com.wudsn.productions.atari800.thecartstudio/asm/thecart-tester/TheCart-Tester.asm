; The!Cart Tester program
;
; Displays $8000 and $a000 as split screen.
; Use keyboard to change PORTB and The!Cart Enable
; Use START/SELECT to change the bank registers.
;

	org $2000
	icl "..\thecart-menu\CartMenu-Definitions.asm"

	.var bank .word

start
	mva #1 $42	// Critic

	jsr wait

	mwa #dl $230
	mwa $230 $d402

loop	lda $d209
	ora #$01
	sta $d301
	lda $d209
	and #1
	sta $d5a2

	lda $d20a
	ldx $d20a
	sta $8000,x
	eor #$ff
	sta $a000,x
	jsr wait
	
	lda $d01f
	lsr
	bcs no_start
	inw bank
	jmp set_bank
no_start
	lsr
	bcs no_select
	dew bank
no_select

set_bank
;	mwa #$21 $d5a6		;16k mode
;	mwa bank $d5a0
;	mwa bank $d5a3

	
	jmp loop

wait	lda $d40b
	bne wait
wait1	lda $d40b
	beq wait1
	rts

no_interrupt
	sta $d40f
	rti

	.local dl
	.byte $70,$70,$70
	.byte $42,a($8000)
:90	.byte $0f
	.byte $00
	.byte $42,a($A000)
:90	.byte $0f
	.byte $41,a(dl)
	.endl

	run start
	