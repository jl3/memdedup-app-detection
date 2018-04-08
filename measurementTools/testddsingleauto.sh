#!/bin/bash
ITERATIONS=20
INTDD=80
INTNODD=12
LOGPREFIX="a-"

ITER=0
while [ $ITER - lt $ITERATIONS ]
	./testdedup-single-auto $INTDD 1p-1.dat 1p-2.dat "${LOGPREFIX}1p-dup.log" &
	sleep 1s
	./testdedup-single-auto $INTDD 10p-1.dat 10p-2.dat "${LOGPREFIX}10p-dup.log" &
	sleep 1s
	./testdedup-single-auto $INTDD 20p-1.dat 20p-2.dat "${LOGPREFIX}20p-dup.log" &
	sleep 1s
	./testdedup-single-auto $INTDD 30p-1.dat 30p-2.dat "${LOGPREFIX}30p-dup.log" &
	sleep 1s
	./testdedup-single-auto $INTDD 40p-1.dat 40p-2.dat "${LOGPREFIX}40p-dup.log" &
	sleep 1s
	./testdedup-single-auto $INTDD 50p-1.dat 50p-2.dat "${LOGPREFIX}50p-dup.log" &
	sleep 1s
	./testdedup-single-auto $INTDD 60p-1.dat 60p-2.dat "${LOGPREFIX}60p-dup.log" &
	sleep 1s
	./testdedup-single-auto $INTDD 70p-1.dat 70p-2.dat "${LOGPREFIX}70p-dup.log" &
	sleep 1s
	./testdedup-single-auto $INTDD 80p-1.dat 80p-2.dat "${LOGPREFIX}80p-dup.log" &
	sleep 1s
	./testdedup-single-auto $INTDD 90p-1.dat 30p-2.dat "${LOGPREFIX}90p-dup.log" &
	sleep 1s
	./testdedup-single-auto $INTDD 100p-1.dat 100p-2.dat "${LOGPREFIX}100p-dup.log" &
	sleep 1s

	./testdedup-single-auto $INTDD 100p-1d-orig.dat 100p-1d-rand.dat "${LOGPREFIX}100p-1d.log" &
	sleep 1s
	./testdedup-single-auto $INTDD 100p-20d-orig.dat 100p-20d-rand.dat "${LOGPREFIX}100p-20d.log" &
	sleep 1s
	./testdedup-single-auto $INTDD 100p-40d-orig.dat 100p-40d-rand.dat "${LOGPREFIX}100p-40d.log" &
	sleep 1s
	./testdedup-single-auto $INTDD 100p-60d-orig.dat 100p-60d-rand.dat "${LOGPREFIX}100p-60d.log" &
	sleep 1s
	./testdedup-single-auto $INTDD 100p-80d-orig.dat 100p-80d-rand.dat "${LOGPREFIX}100p-80d.log" &
	sleep 1s

	./testdedup-single-auto $INTNODD 1p-3.dat 1p-4.dat "${LOGPREFIX}1p-nondup.log" &
	sleep 1s
	./testdedup-single-auto $INTNODD 10p-3.dat 10p-4.dat "${LOGPREFIX}10p-nondup.log" &
	sleep 1s
	./testdedup-single-auto $INTNODD 20p-3.dat 20p-4.dat "${LOGPREFIX}20p-nondup.log" &
	sleep 1s
	./testdedup-single-auto $INTNODD 30p-3.dat 30p-4.dat "${LOGPREFIX}30p-nondup.log" &
	sleep 1s
	./testdedup-single-auto $INTNODD 40p-3.dat 40p-4.dat "${LOGPREFIX}40p-nondup.log" &
	sleep 1s
	./testdedup-single-auto $INTNODD 50p-3.dat 50p-4.dat "${LOGPREFIX}50p-nondup.log" &
	sleep 1s
	./testdedup-single-auto $INTNODD 60p-3.dat 60p-4.dat "${LOGPREFIX}60p-nondup.log" &
	sleep 1s
	./testdedup-single-auto $INTNODD 70p-3.dat 70p-4.dat "${LOGPREFIX}70p-nondup.log" &
	sleep 1s
	./testdedup-single-auto $INTNODD 80p-3.dat 80p-4.dat "${LOGPREFIX}80p-nondup.log" &
	sleep 1s
	./testdedup-single-auto $INTNODD 90p-3.dat 90p-4.dat "${LOGPREFIX}90p-nondup.log" &
	sleep 1s
	./testdedup-single-auto $INTNODD 100p-3.dat 100p-4.dat "${LOGPREFIX}100p-nondup.log" &
	sleep 70s

	let ITER=ITER+1
done
