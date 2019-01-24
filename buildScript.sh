export GOAL=release
export BINTRAY_USER=askymore
export TRAVIS_BRANCH=master

export TARGET=aarch64-linux-android
sh -c ./scripts/build-$TARGET -j$(nproc || sysctl -n hw.ncpu) $GOAL

export TARGET=arm-linux-androideabi
sh -c ./scripts/build-$TARGET -j$(nproc || sysctl -n hw.ncpu) $GOAL

export TARGET=i686-linux-android
sh -c ./scripts/build-$TARGET -j$(nproc || sysctl -n hw.ncpu) $GOAL

export TARGET=x86_64-linux-android
sh -c ./scripts/build-$TARGET -j$(nproc || sysctl -n hw.ncpu) $GOAL

export TARGET=host
sh -c ./scripts/build-$TARGET -j$(nproc || sysctl -n hw.ncpu) $GOAL
