#!/bin/bash

# This is an example of how the testdedup-single-auto tool can be called from a shell script.
# Generally, measurements should be repeated multiple times to increase accuracy.
# Measurements targeting the same signatures must not be performed in parallel to each other,
# while measurements targeting signatures not sharing any pages with each other can be performed
# in parallel. However, the overwriting should ideally not take place at exactly the same time
# to avoid measurements influencing each other by inducing memory load, i.e. it is best
# to stagger the calls to testdedup-single-auto in a manner similar to this script.

ITERATIONS=20
INTDD=80
INTNODD=12
LOGPREFIX="a-"

ITER=0
while [ $ITER - lt $ITERATIONS ]
	./testdedup-single-auto -i $INTDD -1 1p-1.dat -2 1p-2.dat -l "${LOGPREFIX}1p-dup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTDD -1 10p-1.dat -2 10p-2.dat -l "${LOGPREFIX}10p-dup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTDD -1 20p-1.dat -2 20p-2.dat -l "${LOGPREFIX}20p-dup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTDD -1 30p-1.dat -2 30p-2.dat -l "${LOGPREFIX}30p-dup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTDD -1 40p-1.dat -2 40p-2.dat -l "${LOGPREFIX}40p-dup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTDD -1 50p-1.dat -2 50p-2.dat -l "${LOGPREFIX}50p-dup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTDD -1 60p-1.dat -2 60p-2.dat -l "${LOGPREFIX}60p-dup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTDD -1 70p-1.dat -2 70p-2.dat -l "${LOGPREFIX}70p-dup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTDD -1 80p-1.dat -2 80p-2.dat -l "${LOGPREFIX}80p-dup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTDD -1 90p-1.dat -2 30p-2.dat -l "${LOGPREFIX}90p-dup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTDD -1 100p-1.dat -2 100p-2.dat -l "${LOGPREFIX}100p-dup.log" -c &
	sleep 1s

	./testdedup-single-auto -i $INTDD -1 100p-1d-orig.dat -2 100p-1d-rand.dat -l "${LOGPREFIX}100p-1d.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTDD -1 100p-20d-orig.dat -2 100p-20d-rand.dat -l "${LOGPREFIX}100p-20d.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTDD -1 100p-40d-orig.dat -2 100p-40d-rand.dat -l "${LOGPREFIX}100p-40d.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTDD -1 100p-60d-orig.dat -2 100p-60d-rand.dat -l "${LOGPREFIX}100p-60d.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTDD -1 100p-80d-orig.dat -2 100p-80d-rand.dat -l "${LOGPREFIX}100p-80d.log" -c &
	sleep 1s

	./testdedup-single-auto -i $INTNODD -1 1p-3.dat -2 1p-4.dat -l "${LOGPREFIX}1p-nondup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTNODD -1 10p-3.dat -2 10p-4.dat -l "${LOGPREFIX}10p-nondup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTNODD -1 20p-3.dat -2 20p-4.dat -l "${LOGPREFIX}20p-nondup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTNODD -1 30p-3.dat -2 30p-4.dat -l "${LOGPREFIX}30p-nondup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTNODD -1 40p-3.dat -2 40p-4.dat -l "${LOGPREFIX}40p-nondup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTNODD -1 50p-3.dat -2 50p-4.dat -l "${LOGPREFIX}50p-nondup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTNODD -1 60p-3.dat -2 60p-4.dat -l "${LOGPREFIX}60p-nondup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTNODD -1 70p-3.dat -2 70p-4.dat -l "${LOGPREFIX}70p-nondup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTNODD -1 80p-3.dat -2 80p-4.dat -l "${LOGPREFIX}80p-nondup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTNODD -1 90p-3.dat -2 90p-4.dat -l "${LOGPREFIX}90p-nondup.log" -c &
	sleep 1s
	./testdedup-single-auto -i $INTNODD -1 100p-3.dat -2 100p-4.dat -l "${LOGPREFIX}100p-nondup.log" -c &
	sleep 70s

	let ITER=ITER+1
done
