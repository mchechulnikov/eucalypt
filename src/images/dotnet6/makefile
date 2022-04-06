.DEFAULT_GOAL = test

IMAGE = ddd
CONTAINER = ddd


build:
	@ docker rm -f $(CONTAINER)
	@ docker build -t $(IMAGE) -f ./dockerfile .

run:
	@ docker rm -f $(CONTAINER)
	@ docker run -d -it --name $(CONTAINER) $(IMAGE)

exec:
	@ ./make-exec.sh $(CONTAINER)

rm:
	@ docker rm -f $(CONTAINER)