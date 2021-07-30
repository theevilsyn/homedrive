import datetime
from importlib.resources import path
import os
import uuid
import random
from functools import wraps
from ftplib import FTP
from os import environ

import jwt
from flask import Flask, jsonify, make_response, request, send_file
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy import ForeignKey
from werkzeug.utils import secure_filename
from werkzeug.security import check_password_hash, generate_password_hash

app = Flask(__name__)

ALLOWED_EXTENSIONS = set(['txt', 'pdf', 'png', 'jpg', 'jpeg', 'gif'])

app.config['SECRET_KEY'] = 'thisissecret' ## change this
app.config['SQLALCHEMY_DATABASE_URI'] = f"sqlite:///{environ['DB_PATH']}"
app.config['UPLOAD_FOLDER'] = "/tmp"
app.config['MAX_CONTENT_LENGTH'] = 100 * 1024

db = SQLAlchemy(app)


class User(db.Model):
	id = db.Column(db.Integer, primary_key=True)
	public_id = db.Column(db.String(100), unique=True)
	username = db.Column(db.String(100), unique=True)
	email = db.Column(db.String(100), unique=True)
	password = db.Column(db.String(300))
	admin = db.Column(db.Boolean)
	storage = db.Column(db.Integer)


class Files(db.Model):
	id = db.Column(db.String(100), primary_key=True)
	user_id = db.Column(db.String(100), ForeignKey(User.public_id))
	rack = db.Column(db.Integer)
	name = db.Column(db.String(100))
	size = db.Column(db.Integer)

def allowed_file(filename):
	return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def token_required(function):
	@wraps(function)
	def decorated(*args, **kwargs):
		token = None
		if 'x-access-token' in request.headers:
			token = request.headers['x-access-token']
		if not token:
			return jsonify({'message': 'Token is missing!'}), 401

		try:
			data = jwt.decode(token, app.config['SECRET_KEY'], algorithms=["HS256"])
			current_user = User.query.filter_by(
				public_id=data['public_id']).first()
		except:
			return jsonify({'message': 'Token is invalid!'}), 401
		return function(current_user, *args, **kwargs)
	return decorated


@app.route('/register', methods=['POST'])
def create_user():
	data = request.get_json(force=True)

	email = User.query.filter_by(email=data['email']).first()
	uname = User.query.filter_by(username=data['username']).first()

	if(uname):
		return jsonify({"message": "Username already in use!"})
	elif(email):
		return jsonify({"message": "Email already in use!"})

	hashed_password = generate_password_hash(data['password'], method='sha256')

	new_user = User(public_id=str(uuid.uuid4()),
					username=data['username'], password=hashed_password, email=data['email'], admin=False)
	db.session.add(new_user)
	db.session.commit()

	return jsonify({'message': 'New user created!'})


@app.route('/login', methods=['POST'])
def login():
	auth = request.get_json(force=True)

	if not auth or not auth['username'] or not auth['password']:
		return make_response('Could not verify', 401, {'WWW-Authenticate': 'Basic realm="Login required!"'})

	user = User.query.filter_by(username=auth['username']).first()
	if not user:
		return make_response('Could not verify', 401, {'WWW-Authenticate': 'Basic realm="Login required!"'})

	if check_password_hash(user.password, auth['password']):
		token = jwt.encode({'public_id': user.public_id, 'exp': datetime.datetime.utcnow(
		) + datetime.timedelta(minutes=10)}, app.config['SECRET_KEY'])
		return jsonify({'token': token})

	return make_response('Could not verify', 401, {'WWW-Authenticate': 'Basic realm="Login required!"'})

@app.route('/upload', methods=['POST'])
@token_required
def uploadFile(current_user):
	# check if the post request has the file part
	if 'file' not in request.files:
		resp = jsonify({'message' : 'No file part in the request'})
		resp.status_code = 400
		return resp
	file = request.files['file']
	if file.filename == '':
		resp = jsonify({'message' : 'No file selected for uploading'})
		resp.status_code = 400
		return resp
	if file and allowed_file(file.filename):
		filename = secure_filename(file.filename)
		file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))
		rack = f"rack{random.randint(1,1)}"

		new_file = Files(
			id=str(uuid.uuid4()),
			name=filename,
			user_id = current_user.public_id,
			rack=int(rack[-1]),
			size=os.path.getsize(os.path.join(app.config['UPLOAD_FOLDER'], filename))
		)

		db.session.add(new_file)
		db.session.commit()


		with FTP(rack, rack, passwd=f"passwordforstorage{rack[-1]}") as _, open(os.path.join(app.config['UPLOAD_FOLDER'], secure_filename(filename)), "rb") as __:
			_.storbinary(f"STOR {secure_filename(filename)}", __)
			os.remove(os.path.join(app.config['UPLOAD_FOLDER'], secure_filename(filename)))
		resp = jsonify({'message' : 'File successfully uploaded'})
		resp.status_code = 201
		return resp
	else:
		resp = jsonify({'message' : 'Allowed file types are txt, pdf, png, jpg, jpeg, gif'})
		resp.status_code = 400
		return resp

@app.route("/download/<string:filename>", methods=['GET'])
@token_required
def downloadFile(current_user, filename):
	rack = Files.query.filter_by(name=secure_filename(filename)).first().rack
	with FTP(f"rack{rack}", f"rack{rack}", passwd=f"passwordforstorage{rack}") as _, open(os.path.join(app.config['UPLOAD_FOLDER'], secure_filename(filename)), 'wb') as __:
		_.retrbinary(f"RETR {secure_filename(filename)}", __.write)

	__ = send_file(os.path.join(app.config['UPLOAD_FOLDER'], secure_filename(filename)), attachment_filename=filename) 
	os.remove(os.path.join(app.config['UPLOAD_FOLDER'], secure_filename(filename)))
	return __

@app.route("/list", methods=['GET'])
@token_required
def listFiles(current_user):
	files = Files.query.filter_by(user_id=current_user.public_id).all()
	if(not len(files)):
		return jsonify({"message": "This user has no files."})
	return jsonify({"message": [{"filename":file.name, "size":file.size} for file in files]})

@app.route("/delete/<string:filename>", methods=['POST'])
@token_required
def deleteFile(current_user, filename):
	rack = Files.query.filter_by(name=secure_filename(filename)).first().rack
	with FTP(f"rack{rack}", f"rack{rack}", passwd=f"passwordforstorage{rack}") as _, open(os.path.join(app.config['UPLOAD_FOLDER'], secure_filename(filename)), 'wb') as __:
		_.delete(secure_filename(filename))

	file = Files.query.filter_by(name=secure_filename(filename)).delete()
	db.session.commit()
	return jsonify({"message": "Successfully deleted!"})

if __name__ == '__main__':
	app.run(host="0.0.0.0", debug=True)