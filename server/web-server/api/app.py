import datetime
import os
import uuid
from functools import wraps
from os import environ

import jwt
from flask import Flask, jsonify, make_response, request
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy import ForeignKey
from werkzeug.utils import secure_filename
from werkzeug.security import check_password_hash, generate_password_hash

app = Flask(__name__)

ALLOWED_EXTENSIONS = set(['txt', 'pdf', 'png', 'jpg', 'jpeg', 'gif'])

app.config['SECRET_KEY'] = 'thisissecret'
# app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:////app/todo.db'
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:////Users/f4lc0n/Documents/CTFs/InCTFi21/Pentest/homedrive/server/web-server/api/todo.db'
app.config['UPLOAD_FOLDER'] = "/tmp"
app.config['MAX_CONTENT_LENGTH'] = 100 * 1024

db = SQLAlchemy(app)


class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    public_id = db.Column(db.String(50), unique=True)
    username = db.Column(db.String(50), unique=True)
    email = db.Column(db.String(100), unique=True)
    password = db.Column(db.String(300))
    admin = db.Column(db.Boolean)
    storage = db.Column(db.Integer)


class Files(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.String(50), ForeignKey(User.public_id))
    rack = db.Column(db.Integer)
    path = db.Column(db.String(100))

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
        ) + datetime.timedelta(minutes=30)}, app.config['SECRET_KEY'], algorithms=["HS256"])
        return jsonify({'token': token})

    return make_response('Could not verify', 401, {'WWW-Authenticate': 'Basic realm="Login required!"'})


@app.route('/file-upload', methods=['POST'])
@token_required
def upload(current_user):
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
        ### push to DB
        ### push to Storage Rack
		resp = jsonify({'message' : 'File successfully uploaded'})
		resp.status_code = 201
		return resp
	else:
		resp = jsonify({'message' : 'Allowed file types are txt, pdf, png, jpg, jpeg, gif'})
		resp.status_code = 400
		return resp



if __name__ == '__main__':
    app.run(debug=True)
