import React, { Component } from 'react';
import { LineChart, Line, ResponsiveContainer, Tooltip, Legend, XAxis } from 'recharts';

import Popup from '../components/Popup';
import Card from '../components/Card';
import { get, set } from '../lib/api';
import { toDate } from '../lib/helpers';

const defaultState = {
	neighbors: null,
	newAddress: '',
	formVisible: false,
	error: null,
	removeConfirm: null,
	loading: false
};

class Neighbors extends Component {
	state = Object.assign({}, defaultState);

	componentDidMount() {
		this.init();
	}

	addNeighbor = async (e) => {
		e.preventDefault();

		const { newAddress } = this.state;

		this.setState({
			loading: true
		});

		const { error } = await set('addNeighbor', { address: newAddress });

		if (!error) {
			this.init();
		} else {
			this.setState({
				error,
				loading: false
			});
		}
	};

	removeNeighbor = async () => {
		const { removeConfirm } = this.state;

		this.setState({
			loading: true
		});

		const { error } = await set('removeNeighbor', { address: removeConfirm });

		if (!error) {
			this.init();
		} else {
			this.setState({
				error,
				loading: false
			});
		}
	};

	renderTooltip = (props) => {
		if (props.active && props.payload) {
			const data = props.payload[0].payload;
			return (
				<div className="tooltip">
					<h6>{toDate(props.payload[0].payload.timestamp, true)}</h6>
					{Object.keys(data).map(
						(stat) =>
							stat !== 'timestamp' && (
								<p key={stat}>
									<strong>{data[stat]}</strong> {stat}
								</p>
							)
					)}
				</div>
			);
		}
		return null;
	};

	init = async () => {
		const { neighbors } = await get('neighbors');
		this.setState(Object.assign({}, defaultState, { neighbors }));
	};

	renderGraph = (stats) => (
		<div className="graph">
			<ResponsiveContainer height="100%" width="100%">
				<LineChart data={stats}>
					{Object.keys(stats[0]).map(
						(stat) =>
							stat !== 'timestamp' && (
								<Line key={stat} dot={false} isAnimationActive={false} strokeWidth={2} dataKey={stat} />
							)
					)}
					<XAxis dataKey="timestamp" tickFormatter={toDate} />
					<Tooltip content={this.renderTooltip} />
					<Legend />
				</LineChart>
			</ResponsiveContainer>
		</div>
	);

	render() {
		const { formVisible, neighbors, newAddress, error, removeConfirm, loading } = this.state;

		return (
			<section className="neighbors">
				<h1>
					Neighbors
					<nav>
						<button
							className="button success"
							type="button"
							onClick={() =>
								this.setState({
									formVisible: true
								})
							}
						>
							Add new
						</button>
					</nav>
				</h1>
				{removeConfirm && (
					<Popup
						onConfirm={this.removeNeighbor}
						type="warning"
						cta="Remove"
						loading={loading}
						onClose={() => this.setState({ removeConfirm: null, error: null })}
					>
						Are you sure to remove neighbor <strong>{removeConfirm}</strong>
						{error && <small className="error">{error}</small>}
					</Popup>
				)}
				{formVisible && (
					<Popup
						title="Add neighbour"
						loading={loading}
						onClose={() => this.setState({ formVisible: false, newAddress: '', error: null })}
					>
						<form onSubmit={this.addNeighbor}>
							<fieldset>
								<label htmlFor="newAddress">
									Node address
									<input
										type="text"
										id="newAddress"
										value={newAddress}
										onChange={(e) => this.setState({ newAddress: e.target.value })}
									/>
								</label>
								{error && <small className="error">{error}</small>}
							</fieldset>
							<fieldset className="confirm">
								<button className="button" type="submit">
									Add
								</button>
							</fieldset>
						</form>
					</Popup>
				)}
				{neighbors &&
					neighbors.map(({ address, stats }) => {
						if (!stats.length) {
							stats.push({
								all: 0,
								ignored: 0,
								invalid: 0,
								new: 0,
								requested: 0,
								timestamp: +new Date()
							});
						}

						return (
							<Card title={address} key={address}>
								<nav className="corner">
									<button
										type="button"
										className="button warning small"
										onClick={() => this.setState({ removeConfirm: address })}
									>
										Remove
									</button>
								</nav>
								{this.renderGraph(stats)}
							</Card>
						);
					})}
				{neighbors && neighbors.length === 0 && <h3>No neighbors added</h3>}
			</section>
		);
	}
}

export default Neighbors;
