FROM python

RUN apt update; apt -y --autoremove full-upgrade
ARG DEBIAN_FRONTEND=noninteractive
RUN apt install gcc -y

COPY . /app
WORKDIR /app
RUN pip install -r requirements.txt
EXPOSE 5001
RUN ["python", "-c", "from app import db; db.create_all();exit()"]
CMD ["python", "app.py" ]