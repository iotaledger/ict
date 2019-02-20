export const toDate = (timestamp, long) => {
	const date = new Date(timestamp).toISOString().replace('T', ' ');
	return long ? date.substr(0, 16) : date.substr(-13, 5);
};

export const moduleURI = (name, port) => {
	let url = `${window.location.protocol}//${window.location.hostname}`;
	return port ? `${url}:${port}/` : `${url}:${window.location.port}/modules/${name}/`;
};
